package it.weswit.topdownchain.redirection;

import it.weswit.topdownchain.Chain;
import it.weswit.topdownchain.Launcher;
import it.weswit.topdownchain.RedirectedException;
import it.weswit.topdownchain.ReentrantRedirector;
import it.weswit.topdownchain.StageBase;
import it.weswit.topdownchain.mock.ThreadPoolExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PoolRedirector extends ReentrantRedirector {
    private final ThreadPoolExecutor pool;

    public PoolRedirector(ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    protected Launcher getUniqueLauncher() {
        return new Launcher() {

            @Override
            protected final Object launch(final StageBase stage, final Method method, final Method close, final Object[] args, final Chain chain)
                throws RedirectedException, IllegalAccessException, InvocationTargetException
            {
                if (pool == null) {
                    return runLocally(stage, method, close, args, chain);
                } else {
                    launchProtected(stage, method, close, args, chain);
                    assert(false);
                    return null;
                }
            }
        
            @Override
            protected final void launchProtected(final StageBase stage, final Method method, final Method close, final Object[] args, final Chain chain)
                throws RedirectedException
            {
                if (pool == null) {
                    runProtected(stage, method, close, args, chain);
                } else {
                    // NOTA: il passaggio al pool procura anche un momemto
                    // di sincronizzazione tra i due thread;
                    // cio' serve anche a garantire la sincronizzazione su Chain
                    pool.execute(new Runnable() {
                        public void run() {
                            runRedirected(stage, method, close, args, chain);
                        }
                    });
                    notifyRedirection();
                }
            }

            protected boolean isRedirecting() {
                // come capability in generale, non dipende da force
                return pool != null;
            }
        };
    }
}