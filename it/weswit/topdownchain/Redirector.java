package it.weswit.topdownchain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class Redirector implements BaseRedirector {
    // NOTA: base per implementazioni custom;
    // Possibile solo costruire oggetti usa-e-getta,
    // poiché tiene memoria di ciò che deve fare
    
    private boolean used = false;
    private boolean consumed = false;
    private Chain deferredChain;
    private DeferredLauncher deferredlauncher;
    // NOTA: terremo sincronizzate queste variabili,
    // perche' potrebbero passare di mano da un thread all'altro,
    // anche se un sincronismo e' verosimile che gia' venga
    // effettuato dall'implementazione di launch()
    // all'atto del passaggio del controllo al nuovo thread;
    // cio' serve anche a garantire la sincronizzazione su Chain
    
    public final Launcher getLauncher() {
        synchronized (this) {
            assert(! used);
            used = true;
            deferredlauncher = new DeferredLauncher();
            return deferredlauncher;
        }
    }
    
    protected abstract void launch();

    protected void onCompleted() {
        synchronized (this) {
            assert(used);
            assert(! consumed);
            consumed = true;
            deferredlauncher.defer();
        }
    }

    protected void onDeclaredException(Exception e) {
        // NON C'E' CONTROLLO! DA USARE CON ATTENZIONE!
        assert(! (e instanceof RuntimeException));
        onExceptionInternal(e, true);
    }

    protected void onUnexpectedException(Throwable t) {
        // usata internamente solo per eccezioni non dichiarabili,
        // tuttavia puo' essere invocata da fuori con qualunque eccezione
        onExceptionInternal(t, false);
    }

    private void onExceptionInternal(Throwable t, boolean isDeclared) {
        synchronized (this) {
            assert(used);
            assert(! consumed);
            consumed = true;
            if (isDeclared) {
                deferredChain.setDeclaredThrowable(t);
                    // lasciamo al chiamante il compito di dichiarare l'eccezione
            } else {
                deferredChain.setUndeclaredThrowable(t);
            }
            deferredChain.onClose();
        }
    }

    private class DeferredLauncher extends Launcher {
        private StageBase deferredStage;
        private Method deferredMethod;
        private Method deferredClose;
        private Object[] deferredArgs;
        // NOTA: terremo sincronizzate queste variabili insieme
        // a quelle del Redirector di riferimento (vedi nota relativa)

        @Override
        protected final Object launch(StageBase stage, Method method, Method close, Object[] args, Chain chain)
                throws InvocationTargetException, IllegalAccessException, RedirectedException
        {
            launchProtected(stage, method, close, args, chain);
            assert(false);
            return null;
        }

        @Override
        protected final void launchProtected(StageBase stage, Method method, Method close, Object[] args, Chain chain)
                throws RedirectedException
        {
            synchronized (Redirector.this) {
                deferredStage = stage;
                deferredMethod = method;
                deferredClose = close;
                deferredArgs = args;
                deferredChain = chain;
            }
            Redirector.this.launch();
            // NOTA: niente vieta che launch() abbia chiamato defer o onException
            // in questo stesso thread
            notifyRedirection();
        }
        
        private void defer() {
            // gia' sincronizzato su Redirector.this
            runRedirected(deferredStage, deferredMethod, deferredClose, deferredArgs, deferredChain);
        }
        
        public boolean isRedirecting() {
            return true;
        }
    }
    
    // le seguenti classi non possono essere definite in modo implicito
    // e quindi vanno definite in modo esplicito on-demand;
    // inoltre non possono essere definite ereditando in cascata,
    // perche' devono essere richiamate da Chain in modo non ambiguo

    public static abstract class With1<E1 extends Exception>
        extends Redirector implements BaseRedirector.With1<E1>
    {
        protected void onException1(E1 e) {
            onDeclaredException(e);
                // l'eccezione doveva essere dichiarata in launch();
                // non potendo, deve essere dichiarata da getRedirector;
                // non potendo neanche li', va dichiarata da chi le chiama,
                // cioe' dalle setRedirector in Chain
        }
    }
    
    public static abstract class With2<E1 extends Exception, E2 extends Exception>
        extends Redirector implements BaseRedirector.With2<E1, E2>
    {
        protected void onException1(E1 e) {
            onDeclaredException(e);
        }
        protected void onException2(E2 e) {
            onDeclaredException(e);
        }
    }
    
    public static abstract class With3<E1 extends Exception, E2 extends Exception, E3 extends Exception>
        extends Redirector implements BaseRedirector.With3<E1, E2, E3>
    {
        protected void onException1(E1 e) {
            onDeclaredException(e);
        }
        protected void onException2(E2 e) {
            onDeclaredException(e);
        }
        protected void onException3(E3 e) {
            onDeclaredException(e);
        }
    }
    
    public static abstract class With4<E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception>
        extends Redirector implements BaseRedirector.With4<E1, E2, E3, E4>
    {
        protected void onException1(E1 e) {
            onDeclaredException(e);
        }
        protected void onException2(E2 e) {
            onDeclaredException(e);
        }
        protected void onException3(E3 e) {
            onDeclaredException(e);
        }
        protected void onException4(E4 e) {
            onDeclaredException(e);
        }
    }
    
}
