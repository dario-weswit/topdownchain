package it.weswit.topdownchain.redirection;

import it.weswit.topdownchain.BaseRedirector;
import it.weswit.topdownchain.Chain;
import it.weswit.topdownchain.ChainOutcomeListener;
import it.weswit.topdownchain.FirstStage;
import it.weswit.topdownchain.Launcher;
import it.weswit.topdownchain.RedirectedException;
import it.weswit.topdownchain.ReentrantRedirector;
import it.weswit.topdownchain.StageBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Redirector speciale che può far proseguire l'esecuzione oppure lanciare un'eccezione di timeout.
 * ATTENZIONE! Se scatta il timeout, il chiamante vede solo quello,
 * ma intanto l'esecuzione è proseguita lo stesso in parallelo;
 * quindi questo redirector non può essere usato a piacimento,
 * ma solo in quei casi in cui siamo sicuri che l'esecuzione persa,
 * qualunque strada abbia preso, non possa dare nessun fastidio.
 * Se può servire, si da' l'opportunità di interrompere l'esecuzione e fare rollback
 * prima che il chiamante riprenda;
 * ma l'annullamento degli effetti collaterali deve essere istantaneo:
 * ogni operazione di housekeeping va mandata in asincrono.
 */
public class TimeoutRedirector extends ReentrantRedirector implements BaseRedirector.With1<StageTimeoutException> {

    public interface InterruptibleStage {

        void interruptAndRollbackAsynchronously();

    }
    
    public interface TimeoutChecker {
        
        void start(TimeoutListener listener);
        
    }

    public interface TimeoutListener {
        
        void onTimeout(long timeout);
        
    }

    private final String operation;
    private final BaseRedirector undRedirector;
    private final TimeoutChecker timer;
    
    public TimeoutRedirector(BaseRedirector undRedirector, TimeoutChecker timer, String operation) {
        this.undRedirector = undRedirector;
        this.operation = operation;
        this.timer = timer;
    }

    protected class MyUniqueLauncher extends Launcher {
        
        @Override
        protected Object launch(StageBase stage, Method method, Object[] args, Chain chain)
                throws InvocationTargetException, IllegalAccessException, RedirectedException
        {
            launchProtected(stage, method, args, chain);
            assert(false);
            return null;
        }

        @Override
        protected void launchProtected(final StageBase stage, final Method method, final Object[] args, final Chain baseChain)
                throws RedirectedException
        {
            final AtomicBoolean consumed = new AtomicBoolean(false);
            final AtomicReference<InterruptibleStage> activeStage;
            if (stage instanceof InterruptibleStage) {
                activeStage = new AtomicReference<InterruptibleStage>((InterruptibleStage) stage);
            } else {
                activeStage = null; // inutile tenerlo vivo se non serve
            }
            
            timer.start(new TimeoutListener() {
                public void onTimeout(long timeout) {
                    if (consumed.compareAndSet(false, true)) {
                        onTimedOut(timeout, activeStage, baseChain);
                    } else {
                        // tutto a posto e timeout obsoleto 
                    }
                }
            });

            Chain.startChain(
                new FirstStage() {
                    public void invoke(Chain chain) throws RedirectedException {
                        assert(args.length >= 1 && args[args.length - 1] == baseChain);
                        Launcher und = undRedirector.getLauncher();
                            // viene estratto qui anziché dalla Chain
                        launchOnNewChain(und, stage, method, args, chain);
                    }
                },
                new ChainOutcomeListener() {
                    public void onClose(Throwable currThrown) {
                        if (consumed.compareAndSet(false, true)) {
                            onClosed(currThrown, activeStage, baseChain);
                        } else {
                            onCancelled(currThrown, activeStage, baseChain);
                        }
                    }
                },
                getChainLogger(baseChain)
            );

            // la nuova Chain assume interamente il controllo del flusso
            notifyRedirection();
        }
        
        @Override
        protected boolean isRedirecting() {
            return true;
        }

        protected void onClosed(Throwable currThrown, AtomicReference<InterruptibleStage> activeStage, Chain baseChain) {
            if (activeStage != null) {
                activeStage.set(null); // inutile tenerlo vivo se non serve più
            }
            closeChain(baseChain, currThrown);
        }
        
        protected void onCancelled(Throwable currThrown, AtomicReference<InterruptibleStage> activeStage, Chain baseChain) {
            // già interrotto: tutto ciò che è stato fatto dopo è sprecato
            if (activeStage != null) {
                assert(activeStage.get() == null);
            }
        }
        
        protected void onTimedOut(long timeout, AtomicReference<InterruptibleStage> activeStage, Chain baseChain) {
            if (activeStage != null) {
                InterruptibleStage origStage = activeStage.get();
                activeStage.set(null); // inutile tenerlo vivo se non serve più
                if (origStage != null) {
                    try {
                        origStage.interruptAndRollbackAsynchronously();
                            // NOTA: se nel frattempo questo stage ne ha chiamati altri,
                            // sta a lui prodigarsi per interromperli o rinunciare a farlo
                    } catch (Throwable t) {
                        // ATTENZIONE! ricordarsi di non coprire eccezioni dichiarate
                        assert(false);
                    }
                }
            }
            closeChain(baseChain, new StageTimeoutException(operation, timeout));
        }
    }

    @Override
    protected Launcher getUniqueLauncher() {
        return new MyUniqueLauncher();
    }

}

