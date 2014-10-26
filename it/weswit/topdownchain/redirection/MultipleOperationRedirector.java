package it.weswit.topdownchain.redirection;

import it.weswit.topdownchain.Redirector;
import it.weswit.topdownchain.mock.ThreadPoolExecutor;

public class MultipleOperationRedirector {
    
    public class Semaphore {
        protected Counter remaining;
        
        protected void setCounter(Counter remaining) {
            this.remaining = remaining;
        }
        
        protected Counter getCounter() {
            return remaining;
        }
        
        public void setThrowable(Throwable thr) {
            MultipleOperationRedirector.this.onInstanceFinished(remaining, null, thr);
        }
        
        public void setCompleted() {
            MultipleOperationRedirector.this.onInstanceFinished(remaining, null, null);
        }
    }    

    private interface Base {
        ThreadPoolExecutor getInstancePool(int instance);
        boolean runNonStagedInstance(int instance, Semaphore semaphore) throws Exception;

        void internalOnCompleted();
        void internalOnDeclaredException(Exception exc);
        void internalOnUnexpectedException(Throwable thr);
    }
    
    private final int instances;
    private final Base base;

    MultipleOperationRedirector(int instances, Base impl) {
        this.instances = instances;
        this.base = impl;
    }

    protected final void launch(final Semaphore semaphore) {
        if (instances > 0) {
            final Counter remaining = new Counter();
            remaining.count = instances;
            semaphore.setCounter(remaining);
    
            for (int i = 0; i < instances; i++) {
                final int instance = i;
                ThreadPoolExecutor executionPool = base.getInstancePool(instance);
                executionPool.execute(new Runnable() {
                    public void run() {
                        runLocally(instance, semaphore);
                    }
                });
            }
        } else {
            // gestiamo anche questo caso
            base.internalOnCompleted();
        }
    }
    
    private void runLocally(int instance, Semaphore semaphore) {
        Exception exc = null;
        Throwable thr = null;
        boolean finished = false;
        
        try {
            finished = base.runNonStagedInstance(instance, semaphore);
        
        } catch (RuntimeException t) {
            // deve essere un'eccezione non dichiarabile
            thr = t;
        } catch (Exception e) {
            // non puo' che essere un'eccezione dichiarata in una delle sottoclassi
            exc = e;
        } catch (Error t) {
            // deve essere un'eccezione non dichiarabile
            thr = t;
        } catch (Throwable t) {
            assert(false);
            thr = t;
        }
        
        if (finished) {
            // l'istanza era sincrona ed è già terminata
            onInstanceFinished(semaphore.getCounter(), exc, thr);
        } else {
            // l'istanza era asincrona, segnalerà lei
            // quando sarà terminata attraverso il semaforo
        }
    }
    
    private void onInstanceFinished(Counter remaining, Exception exc, Throwable thr) {
        boolean close;
        synchronized (remaining) {
            if (remaining.count == 0) {
                // significa che l'uscita e' gia' stata anticipata, causa eccezione
                close = false;
            } else if (exc != null || thr != null) {
                // forziamo la terminazione, dovunque siamo;
                // gli altri task correntemente in corso si arrangino
                remaining.count = 0;
                close = true;
            } else {
                remaining.count--;
                if (remaining.count == 0) {
                    close = true;
                } else {
                    close = false; // ci saranno altre occasioni
                }
            }
        }

        if (close) {
            // un solo thread passa da qui
            if (exc != null) {
                // non puo' che essere un'eccezione dichiarata in una delle sottoclassi
                base.internalOnDeclaredException(exc);
            } else if (thr != null) {
                base.internalOnUnexpectedException(thr); // e' un'eccezione non dichiarata
            } else {
                base.internalOnCompleted();
            }
        }
    }
    
    private static class Counter {
        public int count;
    }
    
    public static abstract class With0 extends Redirector implements Base {
        private final MultipleOperationRedirector impl;
        
        public With0(int instances) {
            impl = new MultipleOperationRedirector(instances, this);
        }
        
        public final void run() {
            impl.launch(impl.new Semaphore());
        }
        
        // devono essere definiti e dichiarati pubblici solo ad uso interno,
        // per comodità: per poter essere usati tramite l'interfaccia "Base"
        public final void internalOnCompleted() {
            onCompleted();
        }
        public final void internalOnDeclaredException(Exception e) {
            onDeclaredException(e);
        }
        public final void internalOnUnexpectedException(Throwable t) {
            onUnexpectedException(t);
        }

        /**
         * Returns true if the instance completed its task, false if an async task is still running. 
         */
        public abstract boolean runNonStagedInstance(int instance, Semaphore semaphore);
    }

    public class SemaphoreWith1<E1 extends Exception> extends Semaphore {
        public void setException1(E1 exc1) {
            MultipleOperationRedirector.this.onInstanceFinished(remaining, exc1, null);
        }
    }

    public static abstract class With1<E1 extends Exception> extends Redirector.With1<E1> implements Base {
        private final MultipleOperationRedirector impl;
        
        public class MySemaphore extends SemaphoreWith1<E1> {
            public MySemaphore() {
                impl.super();
            }
        }

        public With1(int instances) {
            impl = new MultipleOperationRedirector(instances, this);
        }
        
        public final void run() {
            impl.launch(new MySemaphore());
        }
        
        // devono essere definiti e dichiarati pubblici solo ad uso interno,
        // per comodità: per poter essere usati tramite l'interfaccia "Base"
        public final void internalOnCompleted() {
            onCompleted();
        }
        public final void internalOnDeclaredException(Exception e) {
            onDeclaredException(e);
        }
        public final void internalOnUnexpectedException(Throwable t) {
            onUnexpectedException(t);
        }

        @SuppressWarnings("unchecked") 
        public final boolean runNonStagedInstance(int instance, Semaphore semaphore) throws E1 {
            return runNonStagedInstance(instance, (MySemaphore) semaphore);
        }
        
        /**
         * Returns true if the instance completed its task, false if an async task is still running. 
         */
        public abstract boolean runNonStagedInstance(int instance, MySemaphore semaphore) throws E1;
    }

    public class SemaphoreWith2<E1 extends Exception, E2 extends Exception> extends SemaphoreWith1<E1> {
        public void setException2(E2 exc2) {
            MultipleOperationRedirector.this.onInstanceFinished(remaining, exc2, null);
        }
    }

    public static abstract class With2<E1 extends Exception, E2 extends Exception>
            extends Redirector.With2<E1, E2> implements Base
    {
        private final MultipleOperationRedirector impl;
        
        public class MySemaphore extends SemaphoreWith2<E1, E2> {
            public MySemaphore() {
                impl.super();
            }
        }

        public With2(int instances) {
            impl = new MultipleOperationRedirector(instances, this);
        }
        
        public final void run() {
            impl.launch(new MySemaphore());
        }
        
        // devono essere definiti e dichiarati pubblici solo ad uso interno,
        // per comodità: per poter essere usati tramite l'interfaccia "Base"
        public final void internalOnCompleted() {
            onCompleted();
        }
        public final void internalOnDeclaredException(Exception e) {
            onDeclaredException(e);
        }
        public final void internalOnUnexpectedException(Throwable t) {
            onUnexpectedException(t);
        }

        @SuppressWarnings("unchecked") 
        public final boolean runNonStagedInstance(int instance, Semaphore semaphore) throws E1, E2 {
            return runNonStagedInstance(instance, (MySemaphore) semaphore);
        }
        
        /**
         * Returns true if the instance completed its task, false if an async task is still running. 
         */
        public abstract boolean runNonStagedInstance(int instance, MySemaphore semaphore) throws E1, E2;
    }

    public class SemaphoreWith3<E1 extends Exception, E2 extends Exception, E3 extends Exception> extends SemaphoreWith2<E1, E2> {
        public void setException3(E3 exc3) {
            MultipleOperationRedirector.this.onInstanceFinished(remaining, exc3, null);
        }
    }

    public static abstract class With3<E1 extends Exception, E2 extends Exception, E3 extends Exception>
            extends Redirector.With3<E1, E2, E3> implements Base
    {
        private final MultipleOperationRedirector impl;
        
        public class MySemaphore extends SemaphoreWith3<E1, E2, E3> {
            public MySemaphore() {
                impl.super();
            }
        }

        public With3(int instances) {
            impl = new MultipleOperationRedirector(instances, this);
        }
        
        public final void run() {
            impl.launch(new MySemaphore());
        }
        
        // devono essere definiti e dichiarati pubblici solo ad uso interno,
        // per comodità: per poter essere usati tramite l'interfaccia "Base"
        public final void internalOnCompleted() {
            onCompleted();
        }
        public final void internalOnDeclaredException(Exception e) {
            onDeclaredException(e);
        }
        public final void internalOnUnexpectedException(Throwable t) {
            onUnexpectedException(t);
        }

        @SuppressWarnings("unchecked") 
        public final boolean runNonStagedInstance(int instance, Semaphore semaphore) throws E1, E2, E3 {
            return runNonStagedInstance(instance, (MySemaphore) semaphore);
        }
        
        /**
         * Returns true if the instance completed its task, false if an async task is still running. 
         */
        public abstract boolean runNonStagedInstance(int instance, MySemaphore semaphore) throws E1, E2, E3;
    }

    public class SemaphoreWith4<E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception> extends SemaphoreWith3<E1, E2, E3> {
        public void setException4(E4 exc4) {
            MultipleOperationRedirector.this.onInstanceFinished(remaining, exc4, null); 
        }
    }

    public static abstract class With4<E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception>
            extends Redirector.With4<E1, E2, E3, E4> implements Base
    {
        private final MultipleOperationRedirector impl;
        
        public class MySemaphore extends SemaphoreWith4<E1, E2, E3, E4> {
            public MySemaphore() {
                impl.super();
            }
        }

        public With4(int instances) {
            impl = new MultipleOperationRedirector(instances, this);
        }
        
        public final void run() {
            impl.launch(new MySemaphore());
        }
        
        // devono essere definiti e dichiarati pubblici solo ad uso interno,
        // per comodità: per poter essere usati tramite l'interfaccia "Base"
        public final void internalOnCompleted() {
            onCompleted();
        }
        public final void internalOnDeclaredException(Exception e) {
            onDeclaredException(e);
        }
        public final void internalOnUnexpectedException(Throwable t) {
            onUnexpectedException(t);
        }

        @SuppressWarnings("unchecked") 
        public final boolean runNonStagedInstance(int instance, Semaphore semaphore) throws E1, E2, E3, E4 {
            return runNonStagedInstance(instance, (MySemaphore) semaphore);
        }
        
        /**
         * Returns true if the instance completed its task, false if an async task is still running. 
         */
        public abstract boolean runNonStagedInstance(int instance, MySemaphore semaphore) throws E1, E2, E3, E4;
    }

}


