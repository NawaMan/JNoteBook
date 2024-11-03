package nawaman.jcompiler;

import static java.lang.Thread.startVirtualThread;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static nawaman.jcompiler.OtherMain.Coroutine.thenNull;
import static nawaman.jcompiler.OtherMain.Coroutine.thenRepeat;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class OtherMain {
    
    static interface TailValueSupplier<OUTPUT> {
        
        OUTPUT get();
        
    }
    
    static interface TailValueSupplierFactory<OUTPUT> {
        
        TailValueSupplier<OUTPUT> create();
        
    }
    
    static class Repeater<OUTPUT> implements TailValueSupplierFactory<OUTPUT> {
        
        static class Data<OUTPUT> implements TailValueSupplier<OUTPUT> {
            
            private OUTPUT value = null;
            
            void set(OUTPUT value) {
                this.value = value;
            }
            
            @Override
            public OUTPUT get() {
                return value;
            }
            
        }
        
        @Override
        public TailValueSupplier<OUTPUT> create() {
            return new Repeater.Data<>();
        }
        
    }
    
    // Constant and throw
    
    static class CoroutineInstance<INPUT, OUTPUT> implements AutoCloseable {
        
        private final Object NULL = new Object();
        
        private final Coroutine<INPUT, OUTPUT>  coroutine;
        
        private volatile Thread thread;
        
        private final TailValueSupplier<OUTPUT>           tail;
        private final Coroutine.Controller<INPUT, OUTPUT> controller;
        
        private final LinkedBlockingQueue<Object> inputQueue  = new LinkedBlockingQueue<>(1);
        private final LinkedBlockingQueue<Object> outputQueue = new LinkedBlockingQueue<>(1);
        
        @SuppressWarnings("unchecked")
        CoroutineInstance(Coroutine<INPUT, OUTPUT>  coroutine, boolean isVirtual) {
            this.coroutine  = coroutine;
            this.tail       = (TailValueSupplier<OUTPUT>)requireNonNullElseGet(coroutine.tail, () -> thenNull()).create();
            this.controller = this::toCaller;
            this.thread     = newThread(isVirtual);
        }
        
        public synchronized OUTPUT next(INPUT input) throws InterruptedException {
//            What to be able to nesure only one next is called at a given time
//            Also make sure exception is only needed.
            System.out.println("Enter by: " + Thread.currentThread());
            synchronized(this) {
                var next = (controller == null) ? toCoroutine(true,  input)
                         : (thread     != null) ? toCoroutine(false, input)
                         :                        tail.get();
                
                if (tail instanceof Repeater.Data<OUTPUT> repeater)
                    repeater.set(next);
                
                System.out.println("Exit by: " + Thread.currentThread());
                return next;
            }
        }
        
        public OUTPUT next() throws InterruptedException {
            return next(null);
        }
        
        @Override
        public void close() throws Exception {
            doClose(null);
        }
        
        //== Internal ==
        
        private Thread newThread(boolean isVirtual) {
            var threadBody = (Runnable)() -> {
                try {
                    var lastValue = coroutine.body.run(null, this.controller);
                    
                    if (tail instanceof Repeater.Data<OUTPUT> repeater)
                        repeater.set(lastValue);
                    
                    doClose(lastValue);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore the interrupt status
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            
            if (isVirtual)
                return startVirtualThread(threadBody);
            
            var thread = new Thread(threadBody);
            thread.start();
            return thread;
        }
        
        private OUTPUT toCoroutine(boolean isFirst, INPUT input) throws InterruptedException {
            if (!isFirst) {
                outputQueue.clear();
                inputQueue.put(wrapNULL(input));
            }
            var output = outputQueue.take();  
            return unwrapNULL(output);
        }
        
        private INPUT toCaller(OUTPUT output) throws InterruptedException {
            if (thread == null)
                // TODO - Find the way to print the coroutine or instance to individualize it
                throw new IllegalStateException("A co-routine is already closed.");
            if (thread != Thread.currentThread())
                // TODO - Find the way to print the coroutine or instance to individualize it
                throw new IllegalStateException(
                        "Co-routine controller's methods can only be called from outside of its thread.");
            
            outputQueue.put(wrapNULL(output));
            var takenInput = inputQueue.take();
            return unwrapNULL(takenInput);
        }
        
        private void doClose(OUTPUT lastValue) throws InterruptedException {
            var thisThread = (Thread)null;
            
            synchronized (this) {
                thisThread = thread;
                thread = null;
                if (thisThread == null)
                    return;
            }
            
            try {
                // Since the output is still waiting, we must return now.
                outputQueue.put(wrapNULL(lastValue));
                
                thisThread.interrupt();
                thisThread.join();
                thisThread = null;
            } finally {
                inputQueue.clear();
                outputQueue.clear();
            }
        }
        
        @SuppressWarnings("unchecked")
        private <T> T unwrapNULL(Object data) {
            return (data == NULL) ? null : (T)data;
        }
        
        private Object wrapNULL(Object data) {
            return (data == null)? NULL : data;
        }
    }
    
    static class Coroutine<INPUT, OUTPUT> {
        
        static interface Controller<INPUT, OUTPUT> {
            
            INPUT yield(OUTPUT output) throws InterruptedException;
            
        }
        
        static interface Body<INPUT, OUTPUT> {
            
            OUTPUT run(Controller<INPUT, OUTPUT> yield) throws InterruptedException;
            
        }
        
        public static interface BodyWithFirst<INPUT, OUTPUT> extends Body<INPUT, OUTPUT> {
            
            public static <I, O> BodyWithFirst<I, O> fromRun(Body<I, O> run) {
                return (firstInput, controller) -> run.run(controller);
            }
            
            OUTPUT run(INPUT firstInput, Controller<INPUT, OUTPUT> controller) throws InterruptedException;
            
            default OUTPUT run(Controller<INPUT, OUTPUT> controller) throws InterruptedException {
                return run(null, controller);
            }
            
        }
        
        @SuppressWarnings("rawtypes")
        private static TailValueSupplier THEN_NULL = () -> null;
        
        @SuppressWarnings("unchecked")
        public static <O> TailValueSupplierFactory<O> thenNull() {
            return (TailValueSupplierFactory<O>)() -> THEN_NULL;
        };
        
        public static <O> TailValueSupplierFactory<O> thenRepeat() {
            return new Repeater<O>();
        };
        
        private final BodyWithFirst<INPUT, OUTPUT>     body;
        private final TailValueSupplierFactory<OUTPUT> tail;
        
        public Coroutine(Body<INPUT, OUTPUT> body, TailValueSupplierFactory<OUTPUT> tail) {
            this(BodyWithFirst.fromRun(body), tail);
        }
        
        public Coroutine(BodyWithFirst<INPUT, OUTPUT> body, TailValueSupplierFactory<OUTPUT> tail) {
            this.tail = requireNonNullElseGet(tail, () -> thenNull());
            this.body = requireNonNull(body, "`body` must not be null.");
        }
        
        public Coroutine(BodyWithFirst<INPUT, OUTPUT> run) {
            this(run, null);
        }
        
        public CoroutineInstance<INPUT, OUTPUT> start() {
            return this.start(true);
        }
        
        public CoroutineInstance<INPUT, OUTPUT> start(boolean isVirtual) {
            return new CoroutineInstance<>(this, isVirtual);
        }
        
    }
    
    private static void println(Object message) {
        synchronized (OtherMain.class) {
            System.out.println(message);
        }
    }
    
    
    public static void main(String[] args) throws Exception {
        var oddNumberGeneratorCoroutine = new Coroutine<Integer, Integer>(controller -> {
            int i = 1;
            while (i < 1000) {
                var skipTo = controller.yield(i);
                i = (skipTo != null) ? (skipTo / 2)*2 + 1 : i + 2;
            }
            return 999;
        }, thenRepeat());
        
        timed(() -> {
            try {
                var oddNumbers = oddNumberGeneratorCoroutine.start();
                
                println(oddNumbers.next());
                println(oddNumbers.next());
                println(oddNumbers.next());
                
                println(oddNumbers.next(120));
                println(oddNumbers.next());
                println(oddNumbers.next());
                
                println(oddNumbers.next(996));
                println(oddNumbers.next());
                
                println(oddNumbers.next());
                
                oddNumbers.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            try {
                var oddNumbers = oddNumberGeneratorCoroutine.start();
                println(oddNumbers.next());
                println(oddNumbers.next());
                println(oddNumbers.next());
                oddNumbers.close();
                println(oddNumbers.next());
                println(oddNumbers.next());
                println(oddNumbers.next());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
//        
//        timed(() -> {
//            try {
//                var oddNumbers = oddNumberGeneratorCoroutine.start();
//                
//                println(oddNumbers.next());
//                println(oddNumbers.next());
//                println(oddNumbers.next());
//                
//                println(oddNumbers.next(120));
//                println(oddNumbers.next());
//                println(oddNumbers.next());
//                
//                println(oddNumbers.next(996));
//                println(oddNumbers.next());
//                
//                println(oddNumbers.next());
//                
//                oddNumbers.close();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//            
//            try {
//                var oddNumbers = oddNumberGeneratorCoroutine.start();
//                println(oddNumbers.next());
//                println(oddNumbers.next());
//                println(oddNumbers.next());
//                oddNumbers.close();
//                println(oddNumbers.next());
//                println(oddNumbers.next());
//                println(oddNumbers.next());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//        
//        timed(() -> {
//            for (int i = 0; i < 100; i++) {
//                println(i*2 + 1);
//            }
//        });
//        
//        timed(() -> {
//            try {
//                var oddNumbers = oddNumberGeneratorCoroutine.start();
//                for (int i = 0; i < 100; i += 2) {
//                    println(oddNumbers.next());
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//        
//        var coroutineReference = new AtomicReference<Coroutine.Controler<Integer, Integer>>();
//        
//        var evenNumberGeneratorCoroutine = new Coroutine<Integer, Integer>(controller -> {
//            coroutineReference.set(controller);
//            
//            int i = 0;
//            while (i < 1001) {
//                var skipTo = controller.yield(i);
//                i = (skipTo != null) ? (skipTo / 2)*2 : i + 2;
//            }
//            return 1000;
//        }, thenRepeat());
//        
//        timed(() -> {
//            try {
//                var evenNumbers = evenNumberGeneratorCoroutine.start();
//                for (int i = 0; i < 100; i += 2) {
//                    println(evenNumbers.next());
//                }
//                evenNumbers.close();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
    }
    
    static void timed(Runnable runnable) {
        var startTime = System.nanoTime();
        
        runnable.run();
        
        System.out.println((System.nanoTime() - startTime) + " ns");
        System.out.println();
        
    }
    
}
