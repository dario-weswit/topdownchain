package it.weswit.topdownchain.test;
import it.weswit.topdownchain.Chain;
import it.weswit.topdownchain.ChainOutcomeListener;
import it.weswit.topdownchain.FirstStage;
import it.weswit.topdownchain.FullStage;
import it.weswit.topdownchain.LogConsumer;
import it.weswit.topdownchain.RedirectedException;
import it.weswit.topdownchain.Redirector;
import it.weswit.topdownchain.SimpleStage;

public class Usage {

    
    
    // ESPRESSIONE TOP-DOWN DEL FLUSSO DI CONTROLLO, IN FORMA TAIL-RICORSIVA:
    
    // placeholders

    private static class RN {}

    private static class RM {}

    private static class TN1 {}

    private static class TN2 {}

    private static class TM1 {}

    private static class TP1 {}

    private static class TQ1 {}

    private static class TR1 {}

    private static class TA1 {}

    private static class TL1 {}

    private static class EC1 extends Exception {}

    private static class EM1 extends Exception {}

    private static class EH1 extends Exception {}
    
    private static void operationalStuff() {}
    
    private static void handlingStuff() {}
    
    private static void closingStuff() {}
    
    private static void preparationStuff() {}
    
    private static void terminationStuff() {}

    // caso normale:
    
    public static class NormalCaseContext {
        
        public void layerN(TN1 vn1) throws EC1, EH1 {
            try {
                TM1 vm1 = null;
                operationalStuff(); // represents normal instructions
                        // uses vn1, vm1
                        // throws EC1, EM1
                layerM(vm1); // last instruction of the block
            } catch (EM1 em1) {
                handlingStuff(); // represents normal instructions
                        // uses vn1, em1
                        // throws EC1, EH1
            } finally {
                closingStuff(); // represents normal instructions
                        // uses vn1
                        // throws EC1, EH1
            }
        }

    }

    // caso retry:
    
    public static class RetryCaseContext {
        
        public void layerN(TN1 vn1) throws EC1, EH1 {
            try {
                TM1 vm1 = null;
                operationalStuff(); // represents normal instructions
                        // uses vn1, vm1
                        // throws EC1, EM1
                layerM(vm1); // last instruction of the block
            } catch (EM1 em1) {
                TP1 vp1 = null;
                handlingStuff(); // represents normal instructions
                        // uses vn1, vp1, em1
                        // throws EC1, EH1
                layerP(vp1); // last instruction of the block
            }
        }

    }

    // caso async-close:

    public static class AsyncCloseCaseContext {

        public void layerN(TN1 vn1) throws EC1, EH1 {
            try {
                TM1 vm1 = null;
                operationalStuff(); // represents normal instructions
                        // uses vn1, vm1
                        // throws EC1, EM1
                layerM(vm1); // last instruction of the block
            } catch (EM1 em1) {
                handlingStuff(); // represents normal instructions
                        // uses vn1, em1
                        // throws EC1, EH1
            } finally {
                TP1 vp1 = null;
                closingStuff(); // represents normal instructions
                        // uses vn1, vp1
                        // throws EC1, EH1
                layerP(vp1); // last instruction of the block
            }
        }
        
    }

    // caso semplificato:

    public static class SimpleCaseContext {

        public void layerN(TN1 vn1) throws EC1 {
            TQ1 vq1 = null;
            operationalStuff(); // represents normal instructions
                    // uses vn1, vq1
                    // throws EC1
            layerQ(vq1); // last instruction of the block
        }
        
    }

    // chiamata iniziale:

    public static class StartContext {
        
        public void start(TA1 va1) { // enforces that layerA has no throws
            layerA(va1);
        }
        
        public void startAll() {
            TA1 va1 = null;
            preparationStuff(); // represents normal instructions
                    // uses va1
            try {
                start(va1);
            } catch (Throwable t) {
                handlingStuff(); // represents normal instructions
                        // uses va1, t
            } // last instruction of the whole thread
        }

    }

    // casi base per chiamate ricorsive

    private static void layerM(TM1 vm1) throws EC1, EM1 {}
    
    private static void layerP(TP1 vp1) throws EC1, EH1 {}
    
    private static void layerQ(TQ1 vq1) throws EC1 {}
    
    private static void layerA(TA1 va1) {}
    
    
    
    // FORMA DA USARE NEI VARI CASI INTRODOTTI SOPRA:
    
    // more placeholders
    
    private static Redirector myRedirectorA;

    private static Redirector myRedirectorB;
    
    // caso normale:

    public static class NormalCaseImplContext {
        
        public static abstract class StageN extends FullStage {
            @StageBody
            public RM body(TN1 vn1, Chain chain) throws EC1, EM1, RedirectedException {
                TM1 vm1 = null;
                operationalStuff(); // represents normal instructions
                    // uses vn1, vm1
                    // throws EC1, EM1
                chain.setRedirector(myRedirectorA);
                return myStageMImpl.invoke(vm1, chain); // last instruction of the block
            }
            @StageInvocation
            public RN invoke(TN1 vn1, Chain chain) throws EC1, EH1, RedirectedException {
                try {
                    RM ret = body(vn1, chain);
                    return null;
                } catch (EM1 em1) {
                    handlingStuff(); // represents normal instructions
                        // uses vn1, em1
                        // throws EC1, EH1
                    return null;
                } finally {
                    closingStuff(); // represents normal instructions
                        // uses vn1
                        // throws EC1, EH1
                }
            }
        }
        
    }

    // caso retry:

    public static class RetryCaseImplContext {
        
        public static abstract class StageN extends FullStage {
            @StageBody
            public RM body(TN1 vn1, Chain chain) throws EC1, EM1, RedirectedException {
                TM1 vm1 = null;
                operationalStuff(); // represents normal instructions
                    // uses vn1, vm1
                    // throws EC1, EM1
                chain.setRedirector(myRedirectorA);
                return myStageMImpl.invoke(vm1, chain); // last instruction of the block
            }
            @StageInvocation
            public RN invoke(TN1 vn1, Chain chain) throws EC1, EH1, RedirectedException {
                try {
                    RM ret = body(vn1, chain);
                    return null;
                } catch (EM1 em1) {
                    TP1 vp1 = null;
                    handlingStuff(); // represents normal instructions
                        // uses vn1, vp1, em1
                        // throws EC1, EH1
                    chain.setRedirector(myRedirectorB);
                    return myStagePImpl.invoke(vp1, chain); // last instruction of the block
                }
            }
        }
        
    }

    // caso async-close:

    public static class AsyncCloseImplContext {
        
        public static abstract class StageN extends FullStage {
            @StageBody
            public RM body(TN1 vn1, Chain chain) throws EC1, EM1, RedirectedException {
                TM1 vm1 = null;
                operationalStuff(); // represents normal instructions
                    // uses vn1, vm1
                    // throws EC1, EM1
                chain.setRedirector(myRedirectorA);
                return myStageMImpl.invoke(vm1, chain); // last instruction of the block
            }
            @StageInvocation
            public RN invoke(TN1 vn1, Chain chain) throws EC1, EH1, RedirectedException {
                try {
                    RM ret = body(vn1, chain);
                    return null;
                } catch (EM1 em1) {
                    handlingStuff(); // represents normal instructions
                        // uses vn1, em1
                        // throws EC1, EH1
                    return null;
                } finally {
                    TQ1 vq1 = null;
                    closingStuff(); // represents normal instructions
                        // uses vn1, vq1
                        // throws EC1, EM1
                    myStageQImpl.finallyCheck();
                    chain.setRedirector(myRedirectorB);
                    myStageQImpl.invoke(vq1, chain); // last instruction of the block
                }
            }
        }
        
    }

    // caso semplificato:

    public static class SimpleCaseImplContext {
        
        public static abstract class StageN extends SimpleStage {
            @StageInvocation
            public RN invoke(TN1 vn1, Chain chain) throws EC1, EH1, RedirectedException {
                TP1 vp1 = null;
                operationalStuff(); // represents normal instructions
                    // uses vn1, vp1
                    // throws EC1
                chain.setRedirector(myRedirectorA);
                return myStagePImpl.invoke(vp1, chain); // last instruction of the block
            }
        }
        
    }

    // chiamata iniziale:

    public static class StartImplContext {
        
        public void startAll() {
            TA1 va0 = null;
            preparationStuff(); // represents normal instructions
                // uses va0
            final TA1 va1 = va0;
            Chain.startChain(
                new FirstStage() {
                    public void invoke(Chain chain) throws RedirectedException {
                        chain.setRedirector(myRedirectorA);
                        myStageAImpl.invoke(va1, chain);
                    }
                },
                new ChainOutcomeListener() {
                    public void onClose(Throwable thrown) {
                        if (thrown != null) {
                            handlingStuff(); // represents normal instructions
                                // uses va1, thrown
                        }
                        terminationStuff(); // represents normal instructions
                            // uses va1
                    }
                },
                new LogConsumer() {
                    public void error(String msg, Throwable t) {}
                }
            ); // last instruction of the whole thread
        }
        
    }

    // casi base per chiamate ricorsive

    public static abstract class StageM extends SimpleStage {
        @StageInvocation
        public RM invoke(TM1 vm1, Chain chain) throws EC1, EM1 {
            return null;
        }
    }
    
    private static StageM myStageMImpl;
    
    public static abstract class StageP extends SimpleStage {
        @StageInvocation
        public RN invoke(TP1 vp1, Chain chain) throws EC1, EH1 {
            return null;
        }
    }
    
    private static StageP myStagePImpl;
    
    public static abstract class StageQ extends SimpleStage {
        @StageInvocation
        public void invoke(TQ1 vq1, Chain chain) throws EC1, EH1 {}
    }
    
    private static StageQ myStageQImpl;
    
    public static abstract class StageA extends SimpleStage {
        @StageInvocation
        public void invoke(TA1 va1, Chain chain) {}
    }
    
    private static StageA myStageAImpl;

}
