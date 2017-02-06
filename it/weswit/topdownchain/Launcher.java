package it.weswit.topdownchain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

public abstract class Launcher {
    // Nota: Base per implementazioni interne efficienti
    // in quanto riutilizzabili; per poter essere riutilizzabili,
    // le implementazioni devono avere certe caratteristiche;
    // l'interfaccia non è in grado di guidare l'implementazione
    // e quindi non può essere usata per implementazioni custom

    /*
     * In caso di completamento torna il risultato o l'eccezione;
     * in caso di redirezione torna la RedirectedException
     */
    protected abstract Object launch(StageBase stage, Method method, Method close, Object[] args, Chain chain)
        throws InvocationTargetException, IllegalAccessException, RedirectedException;

    /*
     * In caso di completamento salva il risultato o l'eccezione e torna;
     * in caso di redirezione torna la RedirectedException
     */
    protected abstract void launchProtected(StageBase stage, Method method, Method close, Object[] args, Chain chain)
        throws RedirectedException;

    protected abstract boolean isRedirecting();

    protected static void launchOnNewChain(Launcher launcher, StageBase stage, Method method, Method close, Object[] args, Chain chain)
        throws RedirectedException
    {
        args[args.length - 1] = chain;
        launcher.launchProtected(stage, method, close, args, chain);
    }

    protected static void runProtected(StageBase stage, Method method, Method close, Object[] args, Chain chain) throws RedirectedException {
        runProtectedInternal(stage, method, close, args, chain);
    }

    static void runProtectedInternal(StageBase stage, Method method, Method close, Object[] args, ChainInternal chain) throws RedirectedException {
        if (close != null) {
            chain.addClosingAction(stage, close, args);
        }
        try {
            Object ret = method.invoke(stage, args);
            if (method.getReturnType() == void.class) {
                assert(ret == null);
                chain.setVoidValue();
            } else {
                chain.setReturnValue(ret);
                // ATTENZIONE! Non sappiamo se il chiamante fa un return del valore ottenuto
                // da questo metodo o no; però sappiamo che questa chiamata è l'ultima cosa
                // che fa, quindi se il chiamante non è void fa per forza un return e questo
                // valore gli serve, mentre se il chiamante è void non fa un return ma nemmeno
                // userà questo valore, quindi non c'è problema a tornarlo.
                // Resta solo un caso: un blocco finally che non fa return anche se il
                // chiamante torna un valore; questo caso va gestito a parte, ma non qui
            }
        } catch (InvocationTargetException e) {
            Throwable thrown = e.getTargetException();
            if (thrown instanceof UndeclaredThrowableException) {
                // possibile se invoke viene implementata dirottandola su body;
                // in tal caso, la cattura dell'eccezione e' gia' prevista nella
                // prima ClosingAction in lista
                thrown = ((UndeclaredThrowableException) thrown).getUndeclaredThrowable();
            }
            if (thrown instanceof RedirectedException) {
                // OK, rediretto, quindi la chain sta proseguendo altrove
                // (o addirittura e' gia' proseguita, in callback, in questo thread)
                throw (RedirectedException) thrown;
            } else if (thrown instanceof RuntimeException) {
                // se siamo qui, vuol dire che e' andato storto qualcosa;
                // e' addirittura possible che in origine sia stata
                // lanciata una RedirectedException
                RedirectedException.checkAndResumeThrown(thrown);
                chain.setUndeclaredThrowable(thrown);
            } else if (thrown instanceof Error) {
                // se siamo qui, vuol dire che e' andato storto qualcosa;
                // e' addirittura possible che in origine sia stata
                // lanciata una RedirectedException
                RedirectedException.checkAndResumeThrown(thrown);
                chain.setUndeclaredThrowable(thrown);
            } else {
                // non puo' che essere un'eccezione dichiarata da method;
                // non e' stata dichiarata qui, ma sappiamo che se siamo qui
                // e' a seguito di un'invocazione esplicita di method
                chain.setDeclaredThrowable(thrown);
            }

        // se l'eccezione e' sull'invocante anziche' sull'invocato,
        // la assimilo ugualmente a un'eccezione sull'invocato

        } catch (IllegalAccessException e) {
            // anche se e' dichiarata sul metodo, non proviene dal metodo
            assert(false);
            chain.setUndeclaredThrowable(e);
        } catch (IllegalArgumentException e) {
            // anche se e' dichiarata sul metodo, non proviene dal metodo
            assert(false);
            chain.setUndeclaredThrowable(e);
        } catch (RuntimeException t) {
            // e' un'eccezione non dichiarata
            chain.setUndeclaredThrowable(t);
        } catch (Error t) {
            // e' un'eccezione non dichiarata
            chain.setUndeclaredThrowable(t);
        } catch (Throwable t) {
            assert(false);
            chain.setUndeclaredThrowable(t);
        }
    }
    
    protected static Object notifyRedirection() throws RedirectedException {
        return RedirectedException.commonThrow();
    }

    protected static LogConsumer getChainLogger(ChainInternal chain) {
        return chain.logger;
    }

    protected static void runRedirected(StageBase stage, Method method, Method close, Object[] args, Chain chain) {
        try {
            runProtected(stage, method, close, args, chain);
        } catch (RedirectedException e) {
            // OK, rediretto, quindi la chain sta proseguendo altrove
            // (o addirittura e' gia' proseguita, in callback, in questo thread)
            RedirectedException.cleanup(e);
            return;
        } finally {
            RedirectedException.cleanupThread(getChainLogger(chain));
        }
        chain.onClose();
    }

    protected static Object runLocally(StageBase stage, Method method, Method close, Object[] args, Chain chain)
            throws InvocationTargetException, IllegalAccessException, RedirectedException
    {
		if (close != null) {
			chain.addClosingAction(stage, close, args);
		}
        // qui il tipo di ritorno deve essere già accertato come coerente,
        // quindi possiamo lasciare il controllo dell'esito al chiamante;
        // l'eccezione però potrebbe non essere compatibile
        // e dovrà essere gestita dal chiamante
        return method.invoke(stage, args);
    }

    protected static void closeChain(Chain baseChain, Throwable currThrown) {
        if (currThrown != null) {
            baseChain.setDeclaredThrowable(currThrown);
        }
        baseChain.onClose();
    }
}