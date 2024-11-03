package nawaman.jcompiler;

import static java.lang.Thread.startVirtualThread;
import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.concurrent.Semaphore;

public class OtherMain2 {
    
    public static class Coroutine implements AutoCloseable {
        
        static enum State {
            LOCK_MAIN,
            LOCK_SUB,
            LOCK_NONE;
        }
        
        public class Context {
            
            /** The coroutine thread releases the main thread and then pause. */
            public final void yield() throws InterruptedException {
                changeState(State.LOCK_SUB);
                subLock.acquire();
            }
            
            /** The coroutine thread releases the main thread and then pause. */
            public final void resume() throws InterruptedException {
                changeState(State.LOCK_NONE);
            }
            
        }
        
        public static interface Body {
            
            void run(Coroutine.Context controller) throws InterruptedException;
            
        }
        
        private final Semaphore mainLock = new Semaphore(1);
        private final Semaphore subLock  = new Semaphore(1);
        
        private final Body body;
        
        private volatile State  state  = State.LOCK_SUB;
        private volatile Thread thread = null;
        
        public Coroutine(Body body) {
            var controller = new Context();
            
            changeState(State.LOCK_SUB);
            
            this.body   = requireNonNull(body);
            this.thread = startVirtualThread(() -> {
                try {
                    subLock.acquire();
                    body.run(controller);
                    changeState(State.LOCK_SUB);
                } catch (InterruptedException e) {
                    // Do nothing.
                }
            });
        }
        
        private synchronized void changeState(State state) {
            var firstAction  = (Runnable)null;
            var secondAction = (Runnable)null;
            this.state = switch(state) {
                case LOCK_MAIN -> {
                    firstAction  = ensureLock(mainLock);
                    secondAction = ensureUnlock(subLock);
                    yield state;
                }
                case LOCK_SUB -> {
                    firstAction  = ensureLock(subLock);
                    secondAction = ensureUnlock(mainLock);
                    yield state;
                }
                case LOCK_NONE -> {
                    firstAction  = ensureUnlock(mainLock);
                    secondAction = ensureUnlock(subLock);
                    yield state;
                }
            };
            
            firstAction.run();
            secondAction.run();
        }
        
        private final Runnable ensureLock(Semaphore lock) {
            return () -> {
                lock.drainPermits();
            };
        }
        
        private final Runnable ensureUnlock(Semaphore lock) {
            return () -> {
                lock.drainPermits();
                lock.release(1); 
            };
        }
        
        public void yield() throws InterruptedException {
            doYield();
        }
        
        public void resume() {
            doRelease();
        }
        
        /** The main thread releases the coroutine execution and then pause. */
        protected final void doYield() throws InterruptedException {
            changeState(State.LOCK_MAIN);
            mainLock.acquire();
        }
        
        /** The main thread releases the coroutine execution then continue running. */
        protected final void doRelease() {
            changeState(State.LOCK_NONE);
        }
        
        @Override
        public void close() throws Exception {
            this.thread.interrupt();
            this.thread.join();
            this.thread = null;
        }
        
    }
    
    
    public static void main(String[] args) throws Exception {
        var coroutine = new Coroutine(ctx -> {
            System.out.println("1");
            System.out.println("2");
            System.out.println("3");
            ctx.yield();
            System.out.println("4");
            System.out.println("5");
        });
        
        System.out.println("A");
        System.out.println("B");
        coroutine.yield();
        System.out.println("C");
        System.out.println("D");
        coroutine.close();
        
        System.out.println("E");
        System.out.println("F");
        
        System.out.println("DONE!");
        Thread.sleep(2000);
    }
    
    static void timed(Runnable runnable) {
        var startTime = System.nanoTime();
        
        runnable.run();
        
        System.out.println((System.nanoTime() - startTime) + " ns");
        System.out.println();
        
    }
    
}
