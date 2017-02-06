package it.weswit.topdownchain.redirection;

import it.weswit.topdownchain.Chain;
import it.weswit.topdownchain.Launcher;
import it.weswit.topdownchain.RedirectedException;
import it.weswit.topdownchain.ReentrantRedirector;
import it.weswit.topdownchain.StageBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public class TimerRedirector extends ReentrantRedirector {
    private final Timer timer;
    private final long targetTime;

    public TimerRedirector(Timer impl, long targetTime) {
        this.timer = impl;
        this.targetTime = targetTime;
    }

    @Override
    protected Launcher getUniqueLauncher() {
        return new Launcher() {

            @Override
            protected final Object launch(final StageBase stage, final Method method, final Method close, final Object[] args, final Chain chain)
                throws RedirectedException, IllegalAccessException, InvocationTargetException
            {
                long now = System.currentTimeMillis();
                if (targetTime <= now) {
                    return runLocally(stage, method, close, args, chain);
                } else {
                    launchProtected(stage, method, close, args, chain);
                    // assert(false); non è detto, se cambia il millisecondo: dobbiamo provvedere
                    closeChain(chain, null);
                    return notifyRedirection();
                }
            }

            @Override
            protected final void launchProtected(final StageBase stage, final Method method, final Method close, final Object[] args, final Chain chain)
                    throws RedirectedException
            {
                long now = System.currentTimeMillis();
                if (targetTime <= now) {
                    runProtected(stage, method, close, args, chain);
                } else {
                    // NOTA: il passaggio al timer procura anche un momemto
                    // di sincronizzazione tra i due thread;
                    // cio' serve anche a garantire la sincronizzazione su Chain
                    timer.schedule(new TimerTask() {
                        public void run() {
                            runRedirected(stage, method, close, args, chain);
                        }
                    }, targetTime - now);
                    notifyRedirection();
                }
            }
            
            protected boolean isRedirecting() {
                // come capability in generale, non dipende da come viene usato
                return true;
            }
        };
    }
}