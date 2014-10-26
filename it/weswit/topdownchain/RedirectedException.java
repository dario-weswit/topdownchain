package it.weswit.topdownchain;

import java.util.LinkedList;
import java.util.List;

public final class RedirectedException extends Throwable {
    RedirectedException() {}
    
    private static RedirectedException myCommonRedirectedException = new RedirectedException();
    
    private static ThreadLocal<RedirectedException> pending = new ThreadLocal<RedirectedException>();
    // tiene memoria della presenza della RedirectedException
    // dal lancio fino al momento della cattura;
    // questo perché è possibile che lungo la catena discendente
    // l'eccezione venga sostituita: in particolare all'uscita
    // dai metodi reflettivi (anche se non è mai stato osservato
    // con Javassist, ma solo nella vecchia implementazione con
    // java.lang.reflect.Proxy, quando le classi eccezione
    // dichiarate dal metodo non erano accessibili)
    
    static Object commonThrow() throws RedirectedException  {
        pending.set(myCommonRedirectedException);
        throw myCommonRedirectedException;
    }

    static void checkAndResumeThrown(Throwable t) throws RedirectedException {
        RedirectedException re = pending.get();
        if (re != null) {
            addSpuriousException(t);
            throw re;
        }
    }

    static boolean checkAndCleanupThrown(Throwable t) {
        RedirectedException re = pending.get();
        if (re != null) {
            addSpuriousException(t);
            pending.set(null);
            return true;
        }
        return false;
    }
    
    static void cleanup(RedirectedException e) {
        RedirectedException re = pending.get();
        pending.set(null);
        if (re == null) {
            addSpuriousException(new RuntimeException("Error in RedirectedException handling"));
        } else if (re != e) {
            addSpuriousException(new RuntimeException("Error in RedirectedException lifecycle"));
        }
    }
    
    static void cleanupThread(LogConsumer logger) {
        RedirectedException re = pending.get();
        pending.set(null);
        if (re != null) {
            addSpuriousException(new RuntimeException("Error in RedirectedException management"));
        }
        consumeSpuriousExceptions(logger);
    }

    private static ThreadLocal<List<Throwable>> spuriousExceptions = new ThreadLocal<List<Throwable>>();
    
    private static void addSpuriousException(Throwable t) {
        if (spuriousExceptions.get() == null) {
            spuriousExceptions.set(new LinkedList<Throwable>());
        }
        spuriousExceptions.get().add(t);
    }

    private static void consumeSpuriousExceptions(LogConsumer logger) {
        List<Throwable> exceptions = spuriousExceptions.get();
        if (exceptions == null) {
            return;
        }
        spuriousExceptions.set(null);
        if (logger != null) {
            for (Throwable t : exceptions) {
                logger.error("Unexpected error in Chain execution", t);
            }
        }
    }
}