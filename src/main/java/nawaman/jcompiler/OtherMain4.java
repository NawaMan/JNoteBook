//package nawaman.jcompiler;
//
//import static java.lang.Thread.startVirtualThread;
//
//import java.util.concurrent.Semaphore;
//
//import functionalj.function.FunctionInvocationException;
//import functionalj.promise.DeferValue;
//import functionalj.promise.HasPromise;
//import functionalj.promise.Promise;
//import functionalj.result.Result;
//
//public class OtherMain4 {
//    
//    public static class Coroutine<DATA, THROWABLE extends Throwable> implements HasPromise<DATA>, AutoCloseable {
//        
//        static enum State {
//            LOCK_MAIN,
//            LOCK_SUB,
//            LOCK_NONE,
//            COMPLETED;
//        }
//        
//        public class Context {
//            
//            /** The coroutine thread releases the main thread and then pause. */
//            public final void yield() throws InterruptedException {
//                changeState(State.LOCK_SUB);
//                subLock.acquire();
//            }
//            
//            /** The coroutine thread releases the main thread and then pause. */
//            public final void resume() {
//                changeState(State.LOCK_NONE);
//            }
//            
//        }
//        
//        @FunctionalInterface
//        public static interface BodyVoid<THROWABLE extends Throwable> extends Body<Void, THROWABLE> {
//            
//            void run(Coroutine<Void, THROWABLE>.Context context) throws THROWABLE;
//            
//            public default Void supply(Coroutine<Void, THROWABLE>.Context context) throws THROWABLE {
//                run(context);
//                return null;
//            }
//            
//        }
//        
//        @FunctionalInterface
//        public static interface Body<DATA, THROWABLE extends Throwable> {
//            
//            DATA supply(Coroutine<DATA, THROWABLE>.Context context) throws THROWABLE;
//            
//        }
//        
//        private final Semaphore mainLock = new Semaphore(1);
//        private final Semaphore subLock  = new Semaphore(1);
//        
//        private final DeferValue<DATA> result = DeferValue.deferValue();
//        
//        private volatile State  state  = State.LOCK_SUB;
//        private volatile Thread thread = null;
//        
//        public static <T extends Throwable> Coroutine<Void, T> from(BodyVoid<T> body) {
//            return new Coroutine<Void, T>(body);
//        }
//        
//        public static <D, T extends Throwable> Coroutine<D, T> from(Body<D, T> body) {
//            return new Coroutine<D, T>(body);
//        }
//        
//        protected Coroutine(Body<DATA, THROWABLE> body) {
//            var ctx = new Context();
//            
//            changeState(State.LOCK_SUB);
//            
//            var coroutineBody = (Body<DATA, THROWABLE>)((body == null) ? (Body<DATA, THROWABLE>)(__->null) : body);
//            this.thread = startVirtualThread(() -> {
//                try {
//                    subLock.acquire();
//                    var value = coroutineBody.supply(ctx);
//                    result.complete(Result.ofValue(value));
//                } catch (InterruptedException e) {
//                    result.abort(e);
//                } catch (Throwable throwable) {
//                    var exception
//                            = (throwable instanceof Exception)
//                            ? (Exception)throwable
//                            : new Exception(throwable);
//                    result.complete(Result.ofException(exception));
//                    ctx.resume();
//                } finally {
//                    changeState(State.COMPLETED);
//                }
//            });
//        }
//        
//        protected final State getState() {
//            return changeState(state);
//        }
//        
//        public boolean isCompleted() {
//            return changeState(state) == State.COMPLETED;
//        }
//        
//        private synchronized State changeState(State state) {
//            if (state == this.state)
//                return this.state;
//            
//            var firstAction  = (Runnable)null;
//            var secondAction = (Runnable)null;
//            this.state = switch(state) {
//                case LOCK_MAIN -> {
//                    firstAction  = ensureLock(mainLock);
//                    secondAction = ensureUnlock(subLock);
//                    yield state;
//                }
//                case LOCK_SUB, COMPLETED -> {
//                    firstAction  = ensureLock(subLock);
//                    secondAction = ensureUnlock(mainLock);
//                    yield state;
//                }
//                case LOCK_NONE -> {
//                    firstAction  = ensureUnlock(mainLock);
//                    secondAction = ensureUnlock(subLock);
//                    yield state;
//                }
//            };
//            
//            firstAction.run();
//            secondAction.run();
//            return this.state;
//        }
//        
//        private final Runnable ensureLock(Semaphore lock) {
//            return () -> {
//                lock.drainPermits();
//            };
//        }
//        
//        private final Runnable ensureUnlock(Semaphore lock) {
//            return () -> {
//                lock.drainPermits();
//                lock.release(1); 
//            };
//        }
//        
//        public void yield() throws InterruptedException {
//            doYield();
//        }
//        
//        public void resume() {
//            doRelease();
//        }
//        
//        /** The main thread releases the coroutine execution and then pause. */
//        protected final void doYield() throws InterruptedException {
//            changeState(State.LOCK_MAIN);
//            mainLock.acquire();
//        }
//        
//        /** The main thread releases the coroutine execution then continue running. */
//        protected final void doRelease() {
//            changeState(State.LOCK_NONE);
//        }
//        
//        public Promise<DATA> toPromise() {
//            return result.getPromise();
//        }
//        
//        @Override
//        public Promise<DATA> getPromise() {
//            return result.getPromise();
//        }
//        
//        public Result<DATA> join() throws InterruptedException {
//            this.thread.join();
//            this.thread = null;
//            return result.getCurrentResult();
//        }
//        
//        public Result<DATA> getResult() {
//            if (this.state == State.LOCK_SUB)
//                resume();
//            
//            return toPromise().getResult();
//        }
//        
//        public Result<DATA> getCurrentResult() {
//            return toPromise().getCurrentResult();
//        }
//        
//        @SuppressWarnings("unchecked")
//        public DATA get() throws InterruptedException, THROWABLE {
//            var result = join();
//            if (result.isValue())
//                return result.get();
//            var throwable = (Throwable)result.getException();
//            try { throwable = (THROWABLE)throwable; }
//            catch (ClassCastException castException) {
//                throw (throwable instanceof RuntimeException)
//                        ? (RuntimeException)throwable
//                        : new FunctionInvocationException(throwable);
//            }
//            throw (THROWABLE)throwable;
//        }
//        
//        @Override
//        public void close() throws InterruptedException {
//            this.thread.interrupt();
//            this.join();
//        }
//        
//    }
//    
//    public static class CoroutineSingleValue<VALUE, DATA, THROWABLE extends Throwable> extends Coroutine<DATA, THROWABLE> {
//        
//        public CoroutineSingleValue(Body<DATA, THROWABLE> body) {
//            super(body);
//        }
//        
//    }
//    
//    
//    public static void main(String[] args) throws Exception {
//        {
//            var coroutine = Coroutine.from(ctx -> {
//                System.out.println("1");
//                System.out.println("2");
//                System.out.println("3");
//                ctx.yield();
//                System.out.println("4");
//                System.out.println("5");
//            });
//            
//            System.out.println("A");
//            System.out.println("B");
//            coroutine.yield();
//            System.out.println("C");
//            System.out.println("D");
//            coroutine.close();
//            
//            System.out.println("E");
//            System.out.println("F");
//            
//            System.out.println();
//            System.out.println("DONE - 1! " + coroutine.getResult());
//            System.out.println();
//        }
//        {
//            var coroutine = Coroutine.from(ctx -> {
//                System.out.println("1");
//                System.out.println("2");
//                System.out.println("3");
//                ctx.yield();
//                System.out.println("4");
//                System.out.println("5");
//                throw new RuntimeException("You shall not pass.");
//            });
//            
//            System.out.println("A");
//            System.out.println("B");
//            coroutine.yield();
//            System.out.println("C");
//            System.out.println("D");
//            coroutine.yield();
//            
//            System.out.println("E");
//            System.out.println("F");
//            coroutine.close();
//            
//            System.out.println();
//            System.out.println("DONE - 2! " + coroutine.getResult());
//            System.out.println();
//        }
//            
//        System.out.println("DONE!");
//        Thread.sleep(2000);
//    }
//    
//    static void timed(Runnable runnable) {
//        var startTime = System.nanoTime();
//        
//        runnable.run();
//        
//        System.out.println((System.nanoTime() - startTime) + " ns");
//        System.out.println();
//        
//    }
//    
//}
