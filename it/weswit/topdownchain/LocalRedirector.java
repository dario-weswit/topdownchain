package it.weswit.topdownchain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LocalRedirector extends ReentrantRedirector {

    protected Launcher getUniqueLauncher() {
        return new Launcher() {
            
            @Override
            protected Object launch(StageBase stage, Method method, Method close, Object[] args, Chain chain)
                throws InvocationTargetException, IllegalAccessException, RedirectedException
            {
                return runLocally(stage, method, close, args, chain);
            }

            @Override
            protected void launchProtected(StageBase stage, Method method, Method close, Object[] args, Chain chain)
                    throws RedirectedException
            {
                runProtected(stage, method, close, args, chain);
            }

            protected boolean isRedirecting() {
                return false;
            }
        };
    }
}