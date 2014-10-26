package it.weswit.topdownchain.test;
// ASYNCHRONOUS DELEGATION CHAIN PATTERN

import it.weswit.topdownchain.BaseRedirector;
import it.weswit.topdownchain.Chain;
import it.weswit.topdownchain.ChainOutcomeListener;
import it.weswit.topdownchain.FirstStage;
import it.weswit.topdownchain.FullStage;
import it.weswit.topdownchain.LocalRedirector;
import it.weswit.topdownchain.LogConsumer;
import it.weswit.topdownchain.PatternException;
import it.weswit.topdownchain.RedirectedException;
import it.weswit.topdownchain.SimpleStage;
import it.weswit.topdownchain.mock.Logger;
import it.weswit.topdownchain.mock.ThreadPoolExecutor;
import it.weswit.topdownchain.redirection.PoolRedirector;

import java.util.concurrent.Semaphore;

public class UnitTest {
    
    // PLACEHOLDERS

    public static class RetryExc extends Exception {}

    public static class BodyExc extends Exception {}

    // forme alternative di stage
    
    public static class RetRet {
        
        public static abstract class NormalStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("forwarding value");
                    return ret;
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
            }
        }
        
        public static abstract class CatchStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("forwarding value");
                    return ret;
                } catch (BodyExc u) {
                    logEvent("caught BodyExc");
                    RetryStageS stage = RetryStageS.getStage(RetryStageS.class);
                    chain.setRedirector(getTestRedirector(outcome));
                    return stage.invoke(outcome, chain);
                }
            }
        }

        public static abstract class FinallyStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("forwarding value");
                    return ret;
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                    // CASO VIETATO, MA VERRA' SCOVATO DA finallyCheck
                    RetryStageS stage = RetryStageS.getStage(RetryStageS.class);
                    try {
                        stage.finallyCheck();
                    } catch (PatternException e) {
                        logEvent("throwing ERROR");
                        throw new ForbiddenException();
                    }
                    chain.setRedirector(getTestRedirector(outcome));
                    stage.invoke(outcome, chain);
                }
            }
        }
        
        public static abstract class ComplStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("consuming value");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
                logEvent("completing");
                RetryStageS stage = RetryStageS.getStage(RetryStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
        }

        public static String test(String test, String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
            switch (test) {
            case "Normal":
                NormalStage stage1 = NormalStage.getStage(NormalStage.class);
                return stage1.invoke(outcome, chain);
            case "Catch":
                CatchStage stage2 = CatchStage.getStage(CatchStage.class);
                return stage2.invoke(outcome, chain);
            case "Finally":
                FinallyStage stage3 = FinallyStage.getStage(FinallyStage.class);
                return stage3.invoke(outcome, chain);
            case "Compl":
                ComplStage stage4 = ComplStage.getStage(ComplStage.class);
                return stage4.invoke(outcome, chain);
            default:
                return new String("????");
            }
        }

    }

    public static class VoidRet {
        
        public static abstract class NormalStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("consuming value");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
            }
        }
        
        public static abstract class CatchStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("consuming value");
                } catch (BodyExc u) {
                    logEvent("caught BodyExc");
                    RetryStageS stage = RetryStageS.getStage(RetryStageS.class);
                    chain.setRedirector(getTestRedirector(outcome));
                    stage.invoke(outcome, chain);
                }
            }
        }
        
        public static abstract class FinallyStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("consuming value");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                    // CASO VIETATO, MA VERRA' SCOVATO DA finallyCheck
                    RetryStageS stage = RetryStageS.getStage(RetryStageS.class);
                    try {
                        stage.finallyCheck();
                    } catch (PatternException e) {
                        logEvent("throwing ERROR");
                        throw new ForbiddenException();
                    }
                    chain.setRedirector(getTestRedirector(outcome));
                    stage.invoke(outcome, chain);
                }
            }
        }
        
        public static abstract class ComplStage extends FullStage {
            @StageBody 
            public String body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStageS stage = CloseStageS.getStage(CloseStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                return stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    String ret = body(outcome, chain);
                    logEvent("consuming value");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
                logEvent("completing");
                RetryStageS stage = RetryStageS.getStage(RetryStageS.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
        }

        public static void test(String test, String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
            switch (test) {
            case "Normal":
                NormalStage stage1 = NormalStage.getStage(NormalStage.class);
                stage1.invoke(outcome, chain);
                return;
            case "Catch":
                CatchStage stage2 = CatchStage.getStage(CatchStage.class);
                stage2.invoke(outcome, chain);
                return;
            case "Finally":
                FinallyStage stage3 = FinallyStage.getStage(FinallyStage.class);
                stage3.invoke(outcome, chain);
                return;
            case "Compl":
                ComplStage stage4 = ComplStage.getStage(ComplStage.class);
                stage4.invoke(outcome, chain);
                return;
            default:
                System.out.println("????");
            }
        }

    }

    public static class RetVoid {
        
        public static abstract class NormalStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("creating value");
                    return "Invoke value";
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
            }
        }
        
        public static abstract class CatchStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("creating value");
                    return "Invoke value";
                } catch (BodyExc u) {
                    logEvent("caught BodyExc");
                    // CASO VIETATO
                    if (true) {
                        logEvent("throwing ERROR");
                        throw new ForbiddenException();
                    }

                    RetryStage stage = RetryStage.getStage(RetryStage.class);
                    chain.setRedirector(getTestRedirector(outcome));
                    stage.invoke(outcome, chain);
                    return "Invoke value";
                }
            }
        }
        
        public static abstract class FinallyStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("creating value");
                    return "Invoke value";
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                    RetryStage stage = RetryStage.getStage(RetryStage.class);
                    stage.finallyCheck();
                    chain.setRedirector(getTestRedirector(outcome));
                    stage.invoke(outcome, chain);
                }
            }
        }
        
        public static abstract class ComplStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public String invoke(String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("body successful");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
                logEvent("completing");
                // CASO VIETATO
                if (true) {
                    logEvent("throwing ERROR");
                    throw new ForbiddenException();
                }

                RetryStage stage = RetryStage.getStage(RetryStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
                return "Invoke value";
            }
        }

        public static String test(String test, String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
            switch (test) {
            case "Normal":
                NormalStage stage1 = NormalStage.getStage(NormalStage.class);
                return stage1.invoke(outcome, chain);
            case "Catch":
                CatchStage stage2 = CatchStage.getStage(CatchStage.class);
                return stage2.invoke(outcome, chain);
            case "Finally":
                FinallyStage stage3 = FinallyStage.getStage(FinallyStage.class);
                return stage3.invoke(outcome, chain);
            case "Compl":
                ComplStage stage4 = ComplStage.getStage(ComplStage.class);
                return stage4.invoke(outcome, chain);
            default:
                return new String("????");
            }
        }

    }

    public static class VoidVoid {
        
        public static abstract class NormalStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("body successful");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
            }
        }
        
        public static abstract class CatchStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("body successful");
                } catch (BodyExc u) {
                    logEvent("caught BodyExc");
                    RetryStage stage = RetryStage.getStage(RetryStage.class);
                    chain.setRedirector(getTestRedirector(outcome));
                    stage.invoke(outcome, chain);
                }
            }
        }
        
        public static abstract class FinallyStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("body successful");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                    RetryStage stage = RetryStage.getStage(RetryStage.class);
                    stage.finallyCheck();
                    chain.setRedirector(getTestRedirector(outcome));
                    stage.invoke(outcome, chain);
                }
            }
        }
        
        public static abstract class ComplStage extends FullStage {
            @StageBody 
            public void body(String outcome, Chain chain) throws BodyExc, RedirectedException {
                CloseStage stage = CloseStage.getStage(CloseStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
            @StageInvocation 
            public void invoke(String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
                try {
                    body(outcome, chain);
                    logEvent("body successful");
                } catch (BodyExc u) {
                    logEvent("forwarding BodyExc");
                    throw u;
                } finally {
                    logEvent("finally");
                }
                logEvent("completing");
                RetryStage stage = RetryStage.getStage(RetryStage.class);
                chain.setRedirector(getTestRedirector(outcome));
                stage.invoke(outcome, chain);
            }
        }

        public static void test(String test, String outcome, Chain chain) throws RetryExc, BodyExc, RedirectedException {
            switch (test) {
            case "Normal":
                NormalStage stage1 = NormalStage.getStage(NormalStage.class);
                stage1.invoke(outcome, chain);
                return;
            case "Catch":
                CatchStage stage2 = CatchStage.getStage(CatchStage.class);
                stage2.invoke(outcome, chain);
                return;
            case "Finally":
                FinallyStage stage3 = FinallyStage.getStage(FinallyStage.class);
                stage3.invoke(outcome, chain);
                return;
            case "Compl":
                ComplStage stage4 = ComplStage.getStage(ComplStage.class);
                stage4.invoke(outcome, chain);
                return;
            default:
                System.out.println("????");
            }
        }

    }

    // supporto
    
    public static class ForbiddenException extends Exception {
        public ForbiddenException() {}
    }
    
    private static void logEvent(String event) {
        System.out.println(event);
    }
    
    public static abstract class CloseStageS extends SimpleStage {
        @StageInvocation 
        public String invoke(String outcome, Chain chain) throws BodyExc, RedirectedException {
            if (outcome.startsWith("exc")) {
                logEvent("throwing BodyExc");
                throw new BodyExc();
            }
            logEvent("returning body value");
            return "Body value";
        }
    }
    
    public static abstract class RetryStageS extends SimpleStage {
        @StageInvocation 
        public String invoke(String outcome, Chain chain) throws RetryExc, RedirectedException {
            if (outcome.endsWith("exc")) {
                logEvent("throwing RetryExc");
                throw new RetryExc();
            }
            logEvent("returning retry value");
            return "Retry value";
        }
    }
    
    public static abstract class CloseStage extends SimpleStage {
        @StageInvocation 
        public void invoke(String outcome, Chain chain) throws BodyExc, RedirectedException {
            if (outcome.startsWith("exc")) {
                logEvent("throwing BodyExc");
                throw new BodyExc();
            }
            logEvent("returning from body");
        }
    }
    
    public static abstract class RetryStage extends SimpleStage {
        @StageInvocation 
        public void invoke(String outcome, Chain chain) throws RetryExc, RedirectedException {
            if (outcome.endsWith("exc")) {
                logEvent("throwing RetryExc");
                throw new RetryExc();
            }
            logEvent("returning from retry");
        }
    }
    
    private static BaseRedirector getTestRedirector(String outcome) {
        if (outcome.indexOf("pool") != -1) {
            return new PoolRedirector(new ThreadPoolExecutor());
        } else {
            return new LocalRedirector();
        }
    }

    public static abstract class VoidInitialStage extends FullStage {
        @StageBody 
        public void body(String type, String test, String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
            if (type.endsWith("Void")) {
                VoidVoid.test(test, outcome, chain);
            } else {
                VoidRet.test(test, outcome, chain);
            }
        }
        @StageInvocation 
        public void invoke(String type, String test, String outcome, Chain chain) throws RedirectedException {
            try {
                body(type, test, outcome, chain);
                System.out.println("value: Void");
            } catch (BodyExc e) {
                System.out.println("exception: " + e);
            } catch (RetryExc e) {
                System.out.println("exception: " + e);
            } catch (ForbiddenException e) {
                System.out.println("FORBIDDEN");
            }
        }
    }

    public static abstract class RetInitialStage extends FullStage {
        @StageBody 
        public String body(String type, String test, String outcome, Chain chain) throws RetryExc, BodyExc, ForbiddenException, RedirectedException {
            if (type.endsWith("Void")) {
                return RetVoid.test(test, outcome, chain);
            } else {
                return RetRet.test(test, outcome, chain);
            }
        }
        @StageInvocation 
        public void invoke(String type, String test, String outcome, Chain chain) throws RedirectedException {
            try {
                String ret = body(type, test, outcome, chain);
                System.out.println("value: " + ret);
            } catch (BodyExc e) {
                System.out.println("exception: " + e);
            } catch (RetryExc e) {
                System.out.println("exception: " + e);
            } catch (ForbiddenException e) {
                System.out.println("FORBIDDEN");
            }
        }
    }

    public static void start(final String type, final String test, final String outcome) {
        printTest(type, test, outcome);
        FirstStage impl = new FirstStage() {
            public void invoke(Chain chain) throws RedirectedException {
                if (type.startsWith("Void")) {
                    VoidInitialStage stage = VoidInitialStage.getStage(VoidInitialStage.class);
                    stage.invoke(type, test, outcome, chain);
                } else {
                    RetInitialStage stage = RetInitialStage.getStage(RetInitialStage.class);
                    stage.invoke(type, test, outcome, chain);
                }
            }
        };
        ChainOutcomeListener closeListener = new ChainOutcomeListener() {
            public void onClose(Throwable thrown) {
                if (thrown != null) {
                    System.out.println("unexpected: " + thrown);
                }
                System.out.println();
                System.out.println();
                System.out.println();
                semNext.release();
            }
        };
        Chain.startChain(impl, closeListener, new MyLogger());
        try {
            semNext.acquire();
        } catch (InterruptedException e) {
        }
    }
    
    private static Semaphore semNext = new Semaphore(0);
    
    private static class MyLogger extends Logger implements LogConsumer {}

    public static void main(String[] args) {
        // UnitTest.start("RetVoid", "Finally", "exc/value");
        for (String test : new String[] {"Normal", "Catch", "Finally", "Compl"}) {
            for (String type : new String[] {"RetRet", "VoidRet", "RetVoid", "VoidVoid"}) {
                for (String outcome : new String[] {"exc/exc", "exc/value", "value/exc", "value/value"}) {
                    UnitTest.start(type, test, outcome);
                    UnitTest.start(type, test, outcome.replace("/", "/pool/"));
                }
            }
        }
        
        /*
        CASO Normal RetRet:
          il ritorno del body passa
          non c'è caso retry
            exc/exc, exc/value: torna eccezione body
            value/exc, value/value: torna valore body
        CASO Normal VoidRet:
          il ritorno del body viene consumato
          non c'è caso retry
            exc/exc, exc/value: torna eccezione body
            value/exc, value/value: scrive valore body e torna void
        CASO Normal RetVoid:
          viene generato un valore di ritorno
          non c'è caso retry
            exc/exc, exc/value: torna eccezione body
            value/exc, value/value: torna valore invoke
        CASO Normal VoidVoid:
          nessun ritorno
          non c'è caso retry
            exc/exc, exc/value: torna eccezione body
            value/exc, value/value: torna void

        CASO Catch RetRet:
          il ritorno del body passa
            exc/exc: torna eccezione retry
            exc/value: torna valore retry
            value/exc, value/value: torna valore body
        CASO Catch VoidRet:
          il ritorno del body viene consumato
            exc/exc: torna eccezione retry
            exc/value: ignora valore retry e torna void
            value/exc, value/value: scrive valore body, ignora valore retry e torna void
        CASO Catch RetVoid:
          viene generato un valore di ritorno
            exc/exc, exc/value: FORBIDDEN
            value/exc, value/value: torna valore invoke
        CASO Catch VoidVoid:
          nessun ritorno
            exc/exc: torna eccezione retry
            exc/value: scrive valore retry e torna void
            value/exc, value/value: torna void

        CASO Finally RetRet:
          il ritorno del body passa
            exc/exc, exc/value: FORBIDDEN
            value/exc, value/value: FORBIDDEN
        CASO Finally VoidRet:
          il ritorno del body viene consumato
            exc/exc, exc/value: FORBIDDEN
            value/exc, value/value: FORBIDDEN
        CASO Finally RetVoid:
          viene generato un valore di ritorno
            exc/exc: torna eccezione retry
            exc/value: torna eccezione body
            value/exc: torna eccezione retry
            value/value: torna valore invoke
        CASO Finally VoidVoid:
          nessun ritorno
            exc/exc: torna eccezione retry
            exc/value: torna eccezione body
            value/exc: torna eccezione retry
            value/value: torna void

        CASO Compl RetRet:
          il ritorno del body passa
            exc/exc, exc/value: torna eccezione body
            value/exc: scrive valore body e torna eccezione retry
            value/value: scrive valore body e torna valore retry
        CASO Compl VoidRet:
          il ritorno del body viene consumato
            exc/exc, exc/value: torna eccezione body
            value/exc: scrive valore body e torna eccezione retry
            value/value: scrive valore body, ignora valore retry e torna void
        CASO Compl RetVoid:
          viene generato un valore di ritorno
            exc/exc, exc/value: torna eccezione body
            value/exc, value/value: FORBIDDEN
        CASO Compl VoidVoid:
          nessun ritorno
            exc/exc, exc/value: torna eccezione body
            value/exc: torna eccezione retry
            value/value: torna void
        */
    }
    
    public static void printTest(String type, String test, String outcome) {
        System.out.println("TEST: " + type + " / " + test + " / " + outcome);
        System.out.println("SPIEGAZIONE:");
        System.out.println("\tChiamo invoke");
        System.out.println("\tChiamo body");
        final String state;
        if (outcome.startsWith("value")) {
            if (type.endsWith("Ret")) {
                System.out.println("\tBody torna un valore");
                if (test.equals("Finally")) {
                    System.out.println("\tChiamo blocco finally");
                    System.out.println("\tERRORE");
                    state = "error";
                } else if (test.equals("Compl")) {
                    System.out.println("\tChiamo completion");
                    if (outcome.endsWith("value")) {
                        System.out.println("\tRetry torna un valore");
                        state = "value";
                    } else {
                        System.out.println("\tRetry torna un'eccezione");
                        state = "exc";
                    }
                } else {
                    state = "value";
                }
            } else {
                System.out.println("\tBody torna void");
                if (test.equals("Finally")) {
                    System.out.println("\tChiamo blocco finally");
                    if (outcome.endsWith("value")) {
                        System.out.println("\tRetry torna void");
                        state = "void";
                    } else {
                        System.out.println("\tRetry torna un'eccezione");
                        state = "exc";
                    }
                } else if (test.equals("Compl")) {
                    System.out.println("\tChiamo completion");
                    if (type.startsWith("Void")) {
                        if (outcome.endsWith("value")) {
                            System.out.println("\tRetry torna void");
                            state = "void";
                        } else {
                            System.out.println("\tRetry torna un'eccezione");
                            state = "exc";
                        }
                    } else {
                        System.out.println("\tERRORE");
                        state = "error";
                    }
                } else {
                    state = "void";
                }
            }
        } else {
            System.out.println("\tBody torna un'eccezione");
            if (test.equals("Catch")) {
                System.out.println("\tChiamo blocco catch");
                if (type.startsWith("Ret") && type.endsWith("Void")) {
                    System.out.println("\tERRORE");
                    state = "error";
                } else {
                    if (outcome.endsWith("value")) {
                        if (type.endsWith("Ret")) {
                            System.out.println("\tRetry torna un valore");
                            state = "value";
                        } else {
                            System.out.println("\tRetry torna void");
                            state = "void";
                        }
                    } else {
                        System.out.println("\tRetry torna un'eccezione");
                        state = "exc";
                    }
                }
            } else if (test.equals("Finally")) {
                System.out.println("\tChiamo blocco finally");
                if (type.endsWith("Ret")) {
                    System.out.println("\tERRORE");
                    state = "error";
                } else {
                    if (outcome.endsWith("value")) {
                        System.out.println("\tRetry torna void");
                        state = "exc";
                    } else {
                        System.out.println("\tRetry torna un'eccezione");
                        state = "exc";
                    }
                }
            } else {
                state = "exc";
            }
        }
        
        if (state.equals("exc")) {
            System.out.println("\tInvoke torna un'eccezione");
        } else if (state.equals("value")) {
            if (type.startsWith("Void")) {
                System.out.println("\tInvoke consuma il valore");
                System.out.println("\tInvoke torna void");
            } else {
                System.out.println("\tInvoke torna un valore");
            }
        } else if (state.equals("void")) {
            if (type.startsWith("Void")) {
                System.out.println("\tInvoke torna void");
            } else {
                System.out.println("\tInvoke genera un valore");
                System.out.println("\tInvoke torna un valore");
            }
        }
    }
}





