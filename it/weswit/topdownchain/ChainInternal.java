package it.weswit.topdownchain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

class ChainInternal {
    
    ChainInternal(LogConsumer logger) {
        this.logger = logger;
    }

    private static interface ClosingAction {
        void close();
    }

    private static interface ResumingAction {
        void resume() throws RedirectedException;
    }
    
    private Launcher currLauncher;
    private static final BaseRedirector localRedirector = new LocalRedirector();

    // conservazione dell'esito di una chiamata lungo la catena di ritorno;
    // ad ogni passo, solo una delle due variabili può essere valorizzata
    private Object pendingReturn = NONE;
    private Throwable pendingThrowable = null;
        // INVARIANTE: se non e' un'eccezione non dichiarabile,
        // e' sempre un'eccezione dichiarata nel contesto in cui e' stata originata,
        // altrimenti deve essere stata incapsulata in una RuntimeException;
        // quando viene valorizzata, prelude alla onClose,
        // poi, durante lo stato closing, tiene traccia dell'eccezione in corso,
        // ma in occasione di una rethrow viene azzerata, per essere eventualmente reimpostata,
        // mentre in occasione di una resume viene azzerata per essere eventualmente ripristinata

    private Throwable currThrown;
    private boolean starting = false;
    private boolean closing = false;
    private ResumingAction pendingResume;
    private LinkedList<ClosingAction> actions = new LinkedList<ClosingAction>();

    // NOTA: per velocizzare, non sincronizziamo su queste variabili,
    // anche se potrebbero passare di mano da un thread all'altro,
    // perche', in tal caso, un sincronismo deve gia' avvenire
    // a cura del Redirector
    // all'atto del passaggio del controllo al nuovo thread;
    // infatti, non appena la competenza passa da un thread all'altro,
    // anche l'uso delle variabili si trasferisce interamente
    
    final LogConsumer logger;
    
    private static final Object NONE = new Object() { public String toString() { return "NONE"; } };
    private static final Object VOID = new Object() { public String toString() { return "VOID"; } };
    
    void setRedirector(BaseRedirector manager) {
        if (manager != null) {
            currLauncher = manager.getLauncher();
        } else {
            currLauncher = null;
        }
    }
    
    private Launcher extractLauncher() {
        if (currLauncher != null) {
            Launcher launcher = currLauncher;
            currLauncher = null;
            return launcher;
        } else {
            return localRedirector.getLauncher();
        }
    }
    
    void setReturnValue(Object ret) {
        // NOTA: possiamo arrivare qui anche se il metodo ospitante
        // è void, perché non sappiamo se nel codice c'è un return
        // che impone allo stage chiamante di tramandare il valore;
        // non c'è problema: se il metodo ospitante è void, ignorerà
        // il valore, altrimenti lo ritornerà;
        // tutto ciò funziona solo perché stiamo eseguendo l'ultima
        // istruzione del metodo ospitante
        assert(pendingReturn == NONE);
        assert(pendingThrowable == null);
            // perche' dopo ogni setReturnValue il controllo dovrebbe
            // arrivare direttamente a una ClosingAction, che lo estrae
        pendingReturn = ret;
    }
    
    void setVoidValue() {
        // segnaliamo così che il metodo chiamato è void (potevamo
        // scoprirlo anche prima, ma così è più semplice);
        assert(pendingReturn == NONE);
        assert(pendingThrowable == null);
            // perche' dopo ogni setReturnValue il controllo dovrebbe
            // arrivare direttamente a una ClosingAction, che lo estrae
        pendingReturn = VOID;
            // questo valore fittizio non può essere usato dal chiamante;
            // ma se il metodo è void sarà senz'altro così
    }
    
    void setDeclaredThrowable(Throwable t) {
        assert(pendingReturn == NONE);
        assert(pendingThrowable == null);
            // perche' dopo ogni setDeclaredThrowable il controllo dovrebbe
            // arrivare direttamente a una ClosingAction, che lo estrae
        pendingThrowable = t;
    }
    
    void setUndeclaredThrowable(Throwable t) {
        // usata internamente solo per eccezioni non dichiarabili,
        // tuttavia puo' essere invocata da fuori con qualunque eccezione
        assert(pendingReturn == NONE);
        assert(pendingThrowable == null);
            // perche' dopo ogni setUndeclaredThrowable il controllo dovrebbe
            // arrivare direttamente a una ClosingAction, che lo estrae
        if (t instanceof RuntimeException || t instanceof Error) {
            pendingThrowable = t;
        } else {
            pendingThrowable = new RuntimeException("Unexpected exception in a stage", t);
        }
    }
    
    Object playback() throws Throwable {
        if (pendingThrowable != null) {
            assert(pendingReturn == NONE);
            Throwable thrown = pendingThrowable;
            pendingThrowable = null;
            currThrown = thrown;
            throw thrown;
        } else {
            assert(pendingReturn != NONE);
            Object returned = pendingReturn;
            pendingReturn = NONE;
            return returned == VOID ? null : returned;
            // NOTA: se il metodo era void, abbiamo introdotto
            // per chiarezza il valore fittizio VOID,
            // ma non possiamo tornarlo qui, perché potrebbe
            // trattarsi del ritorno di una body su una invoke;
            // in ogni caso, è impossibile che il chiamante
            // possa fare uso del valore ricevuto
        }
    }
    
    Throwable getBodyException() {
        return currThrown;
    }
    
    void addClosingAction(final StageBase closingStage, final Method close, final Object[] args) {
        actions.add(new ClosingAction() {
            public void close() {
                assert(closing);
                assert(currThrown == null);
                    // puo' essere impostato dalla ClosingAction tramite rethrow;
                    // serve solo per l'uso interno alla ClosingAction
                try {
                    Launcher.runProtectedInternal(closingStage, close, null, args, ChainInternal.this);
                    currThrown = null;
                        // per semplicita', lo annulliamo anche se c'e' una pendingResume;
                        // l'operazione rilanciata non avra' il supporto di getBodyException
                } catch (RedirectedException e) {
                    assert(false);
                    // perche' quando viene lanciata una RedirectedException
                    // closing e' sempre false;
                    // se all'interno della close devo lanciare un nuovo stage, 
                    // setto una pendingResume e da qui esco bene
                }
            }
        });
    }
    
    void addClosingHook(final ChainOutcomeListener hook) {
        if (closing) {
            throw new PatternException("Call non allowed in catch or finally blocks");
                // per non complicare le condizioni
        }
        actions.add(new ClosingAction() {
            public void close() {
                assert(closing);
                try {
                    hook.onClose(pendingThrowable);
                } catch (RuntimeException t) {
                    // e' un'eccezione non dichiarata
                    pendingThrowable = t;
                } catch (Error t) {
                    // e' un'eccezione non dichiarata
                    pendingThrowable = t;
                } catch (Throwable t) {
                    assert(false);
                    pendingThrowable = new RuntimeException(t);
                }
            }
        });
    }
    
    private void setPendingResume(final StageBase resumingStage, final Method body, final Method close, final Object[] args) {
        assert(pendingResume == null);
            // perche' deve essere consumato subito
        pendingResume = new ResumingAction() {
            public void resume() throws RedirectedException {
                assert(! closing);
                launchNextProtected(resumingStage, body, close, args);
            }
        };
    }
    
    /*
     * In caso di completamento torna il risultato o l'eccezione;
     * in caso di redirezione torna la RedirectedException
     */
    Object launchNext(StageBase currStage, Method body, Method close, Object[] args)
        throws InvocationTargetException, IllegalAccessException, RedirectedException
    {
        if (closing) {
            // qui non possiamo tornare niente, nemmeno come trucco
            // per tentare di distinguere il caso catch o finally,
            // perchè se il metodo prevede un valore di ritorno,
            // qualunque altra cosa torniamo potrebbe provocare
            // un'eccezione, indipendentemente dal fatto che il valore
            // di ritorno venga tornato anche dalla invoke o ignorato;
            // e se siamo in un blocco finally e abbiamo un valore
            // di ritorno o eccezione pendente, una tale eccezione
            // coprirebbe addirittura il corretto esito
            launchNextProtected(currStage, body, close, args);
            return null;
        } else if (starting) {
            // la prima volta non possiamo usare la versione veloce,
            // perché altrimenti dovremmo complicare chain.start
            launchNextProtected(currStage, body, close, args);
            return null;
        } else {
            starting = false;
            Launcher launcher = extractLauncher();
            return launcher.launch(currStage, body, close, args, (Chain) this);
        }
    }
    
    /*
     * In caso di completamento salva il risultato o l'eccezione e torna;
     * in caso di redirezione torna la RedirectedException
     */
    void launchNextProtected(StageBase currStage, Method body, Method close, Object[] args)
        throws RedirectedException
    {
        if (closing) {
            // si tratta di una chiamata a substage nella invoke;
            // possiamo essere in un catch / completion o in un
            // blocco finally e non possiamo saperlo;
            // il problema è che se siamo in un blocco finally
            // può esserci un valore di ritorno o un'eccezione
            // già pendente: continuando il processing, se
            // finissimo in asincrono, li perderemmo
            // 
            // allora facciamo così:
            // facciamo un'esecuzione vuota, così la invoke termina
            // e mostra alla onClose l'eventuale esito pendente;
            // intanto teniamo traccia della vera esecuzione ancora
            // mancante: quando la eseguiremo, capiremo quali valori
            // devono essere tornati
            setPendingResume(currStage, body, close, args);
            // ciò funziona solo perché stiamo per eseguire l'ultima
            // istruzione del metodo invoke ospitante;
            // in particolare, è essenziale che in un blocco finally
            // sia vietato perfino il return dopo un substage,
            // altrimenti, uscendo da qui, il valore di ritorno o
            // eccezione pendente verrebbero persi lo stesso
        } else {
            starting = false;
            Launcher launcher = extractLauncher();
            launcher.launchProtected(currStage, body, close, args, (Chain) this);
        }
    }
        
    void onClose() {
        assert(! actions.isEmpty());
            // la prima action deve gestire gli errori imprevisti
        closing = true;
        try {
            while (! actions.isEmpty()) {
                ChainInternal.ClosingAction action = actions.removeLast();
                action.close();
                assert (pendingThrowable != null || pendingReturn != NONE);
                if (pendingResume != null) {
                    // c'era una chiamata a substage nella invoke
                    // e per integrarla nel modo più comodo abbiamo
                    // posposto il processing;
                    // notare che potrebbe essere in un blocco finally;
                    // se è così (ma non possiamo saperlo), può esserci
                    // un valore di ritorno o un'eccezione pendente
                    ResumingAction resume = pendingResume;
                    pendingResume = null;
                    closing = false;
                    onResume(resume);
                    closing = true; // solo se non e' stato rediretto
                }
            }

            // se sono qui (solo se non sono stato rediretto) sono alla fine
            assert(pendingThrowable == null);
                // infatti la prima action deve gestire gli errori imprevisti
            assert(pendingReturn == VOID);
                // infatti la prima action deve essere di tipo void
                // e senza chiamate a substage pendenti
        } catch (RedirectedException e) {
            // OK, rediretto, quindi la chiusura sta proseguendo altrove
            // (o addirittura e' gia' proseguita, in callback, in questo thread)
            RedirectedException.cleanup(e);
        } finally {
            RedirectedException.cleanupThread(logger);
        }
    }
    
    private void onResume(ResumingAction resume) throws RedirectedException {
        // la resume può essere in un blocco finally o in un catch / completion
        // e non possiamo saperlo

        // se è in un catch / completion, vuol dire che non c'è eccezione
        // o valore di ritorno pendente, quindi vale l'esito del substage;
        // se poi il substage torna un valore ma non c'è return, allora la
        // invoke è void, quindi possiamo comunque portare a valle il
        // valore, che poi verrà ignorato dal chiamante della invoke
        //
        // se è in un finally, può esserci un'eccezione o valore di ritorno
        // pendente; ma anche in questo caso (come da specifiche java),
        // un'eccezione o valore di ritorno dal substage prende sempre la
        // precedenza;
        // se però il substage torna un valore, ma non fa return, allora
        // il valore va ignorato (questo tra l'altro è il caso normale,
        // perché abbiamo già vietato per altri motivi che il substage nel
        // finally faccia return);
        // per semplificare, vietiamo del tutto che in un blocco finally
        // si possa utilizzare un substage che ammette un valore di ritorno
        
        // come conseguenza, se il substage torna un valore non void,
        // siamo in un catch / completion e possiamo usare il valore
        // se invece il substage torna un valore void, potremmo essere
        // in un finally e quindi dobbiamo conservare un eventuale valore
        // di ritorno (nonché un'eventuale eccezione);
        // ma se abbiamo pendente un valore di ritorno void senza eccezione,
        // allora possiamo semplicemente ricadere nel primo caso e tenere
        // sempre per buono il ritorno del substage
        
        if (pendingReturn == VOID && pendingThrowable == null) {
            // possiamo ignorarli
        /* } else if (! resume.isVoid()) {
            potevamo determinarlo staticamente ed evitare anche qui
            di memorizzare i valori, ma per semplificare
            lo determineremo a valle, testando il valore tornato
            (se tornerà un'eccezione, non ce ne sarà bisogno)
        */ } else {
            // fanno fede i valori esistenti, salvo che il substage
            // torni un'eccezione;
            // facciamo così: di ritorno dalla resume, mettiamo una
            // ClosingAction che integra i valori nuovi con gli attuali
            final Object value = pendingReturn;
            final Throwable thrown = pendingThrowable;
            actions.add(new ClosingAction() {
                public void close() {
                    if (pendingThrowable != null) {
                        // lo teniamo, così passa davanti
                        assert (pendingReturn == NONE);
                    } else if (pendingReturn != VOID) {
                        // non possiamo essere in un finally
                        assert(thrown == null);
                        assert(value == null); // nota: caso VOID già intercettato sopra
                        // possiamo usare il valore ottenuto
                    } else {
                        pendingReturn = value;
                        pendingThrowable = thrown;
                    }
                }
            });
        }
        // tutto ciò funziona solo perché stiamo per eseguire l'ultima
        // istruzione del metodo invoke ospitante
        
        pendingReturn = NONE;
        pendingThrowable = null;
        resume.resume();
            // nota: se c'era un'eccezione o valore di ritorno pendente,
            // ora l'abbiamo memorizzata e la gestiremo correttamente,
            // ma in ogni caso, per la JVM non è più pendente;
            // supponiamo che non cambi niente; speriamo
    }

    void start(FirstStage stage, final ChainOutcomeListener closeListener) {
        actions.add(new ClosingAction() {
            public void close() {
                if (closeListener != null) {
                    closeListener.onClose(pendingThrowable);
                }
                if (pendingThrowable != null) {
                    assert(pendingReturn == NONE);
                    pendingThrowable = null;
                    pendingReturn = VOID;
                } else {
                    assert(pendingReturn == VOID);
                }
            }
        });
        starting = true;
        currLauncher = null;
        try {
            // impl e' di tipo semplice: non serve un proxy
            // e quindi non serve neanche che estenda SimpleStage
            stage.invoke((Chain) this);
            // la chiamata al primo stage, se c'è (cioè quasi sicuramente),
            // deve essere stata già gestita, per non complicare qui
        } catch (RedirectedException e) {
            // OK, rediretto, quindi la chain sta proseguendo altrove
            // (o addirittura e' gia' proseguita, in callback, in questo thread)
            RedirectedException.cleanup(e);
            return;
        } catch (RuntimeException t) {
            // e' un'eccezione non dichiarata;
            // e' addirittura possible che in origine sia stata
            // lanciata una RedirectedException
            if (! RedirectedException.checkAndCleanupThrown(t)) {
                setUndeclaredThrowable(t);
            }
        } catch (Error t) {
            // e' un'eccezione non dichiarata;
            // e' addirittura possible che in origine sia stata
            // lanciata una RedirectedException
            if (! RedirectedException.checkAndCleanupThrown(t)) {
                setUndeclaredThrowable(t);
            }
        } catch (Throwable t) {
            assert(false);
            setUndeclaredThrowable(t);
        } finally {
            currLauncher = null;
            // per eventuali uscite in eccezione nel substage
            RedirectedException.cleanupThread(logger);
        }
        onClose();
    }

}

