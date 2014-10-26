package it.weswit.topdownchain.test;
// ASYNCHRONOUS DELEGATION CHAIN PATTERN


import it.weswit.topdownchain.Chain;
import it.weswit.topdownchain.ChainOutcomeListener;
import it.weswit.topdownchain.FirstStage;
import it.weswit.topdownchain.FullStage;
import it.weswit.topdownchain.LocalRedirector;
import it.weswit.topdownchain.LogConsumer;
import it.weswit.topdownchain.RedirectedException;
import it.weswit.topdownchain.SimpleStage;
import it.weswit.topdownchain.mock.AsynchronouSocketChannel;
import it.weswit.topdownchain.mock.Logger;
import it.weswit.topdownchain.mock.ThreadPoolExecutor;
import it.weswit.topdownchain.redirection.PoolRedirector;
import it.weswit.topdownchain.redirection.WriteRedirector;

import java.io.IOException;

public class Example {
    
    // PLACEHOLDERS

    private static class A {}

    private static class B {}

    private static class C {}

    private static class D {}

    private static class E {}

    private static class F {}

    private static class G {}

    public static class T extends Exception {}

    public static class U extends Exception {}

    public static class V extends Exception {}

    // versione semplificata finale:

    public static abstract class Stage6 extends SimpleStage {
        @StageInvocation
        public void invoke(G g, Chain chain) throws V, RedirectedException {
            trace("invoke di Stage6");
            // ....
            log("esco");
        }
    }
    
    public Stage6 stage6 = Stage6.getStage(Stage6.class);

    // versione normale finale:

    public static abstract class Stage5 extends FullStage {
        @StageBody
        public void body(F f, Chain chain) throws T, RedirectedException {
            trace("body di Stage5");
            // ....
            log("lancio T");
            throw new T();
        }
        @StageInvocation
        public void invoke(F f, Chain chain) throws T, RedirectedException {
            trace("invoke (cioe' close) di Stage5");
            try {
                body(f, chain);
            } finally {
                trace("finally di Stage5");
                // ....
                log("ho in corso " + getBodyException(chain));
            }
        }
    }
    
    public Stage5 stage5 = Stage5.getStage(Stage5.class);

    // versione semplificata finale:

    public static abstract class Stage4 extends SimpleStage {
        @StageInvocation
        public void invoke(E e, Chain chain) throws U, RedirectedException {
            trace("invoke di Stage4");
            // ....
            chain.addClosingHook(new ChainOutcomeListener(){
                public void onClose(Throwable currThrown) {
                    trace("closing hook di Stage4 con " + currThrown);
                }                
            });
            log("lancio U");
            throw new U();
        }
    }
    
    public Stage4 stage4 = Stage4.getStage(Stage4.class);

    // versione semplificata:

    public abstract class Stage3 extends SimpleStage {
        @StageInvocation
        public void invoke(D d, Chain chain) throws U, RedirectedException {
            trace("invoke di Stage3");
            E e = null;
            // ....
            // possibile qui un nuovo stage
            log("lancio stage4");
            chain.setRedirector(new PoolRedirector(myPool));
            stage4.invoke(e, chain);
        }
    }
    
    public Stage3 stage3 = Stage3.getStage(Stage3.class, this);

    // versione con retry:

    public abstract class Stage2 extends FullStage {
        @StageBody
        public void body(C c, Chain chain) throws U, RedirectedException {
            trace("body di Stage2");
            D d = null;
            // ....
            // possibile qui un nuovo stage
            log("lancio stage3");
            chain.setRedirector(new PoolRedirector(myPool));
            stage3.invoke(d, chain);
        }
        @StageInvocation
        public void invoke(C c, Chain chain) throws T, RedirectedException {
            trace("invoke (cioe' close) di Stage2");
            try {
                body(c, chain);
            } catch (U e) {
                trace("catch(U) di Stage2");
                F f = null;
                // ....
                // possibile qui un nuovo stage
                log("lancio stage5");
                chain.setRedirector(new LocalRedirector());
                stage5.invoke(f, chain);
            }
        }
    }
    
    public Stage2 stage2 = Stage2.getStage(Stage2.class, this);

    // versione con chiusura asincrona:

    public abstract class Stage1 extends FullStage {
        @StageBody
        public void body(B b, Chain chain) throws T, IOException, RedirectedException {
            trace("body di Stage1");
            C c = null;
            // ....
            // possibile qui un nuovo stage
            log("lancio stage2");
            chain.setRedirector(new WriteRedirector(mySocket, "TEXT1".getBytes()));
            stage2.invoke(c, chain);
        }
        @StageInvocation
        public void invoke(B b, Chain chain) throws T, V, IOException, RedirectedException {
            trace("invoke (cioe' close) di Stage1");
            try {
                body(b, chain);
            } catch (T e) {
                trace("catch(T) di Stage1");
                // ....
                log("lancio V");
                throw new V();
            } finally {
                G g = null;
                trace("finally di Stage1");
                // ....
                // possibile qui un nuovo stage
                log("lancio stage6");
                stage6.finallyCheck();
                chain.setRedirector(new PoolRedirector(myPool));
                stage6.invoke(g, chain);
            }
        }
    }

    public Stage1 stage1 = Stage1.getStage(Stage1.class, this);

    // versione normale:

    public abstract class Stage0 extends FullStage {
        @StageBody
        public void body(A a, Chain chain) throws T, V, IOException, RedirectedException {
            trace("body di Stage0");
            B b = null;
            // ....
            // possibile qui un nuovo stage
            log("lancio stage1");
            chain.setRedirector(new WriteRedirector(mySocket, "TEXT0".getBytes()));
            stage1.invoke(b, chain);
        }
        @StageInvocation
        public void invoke(A a, Chain chain) throws RedirectedException {
            trace("invoke (cioe' close) di Stage0");
            try {
                body(a, chain);
            } catch (T e) {
                trace("catch(T) di Stage0");
                // ....
                log("esco");
            } catch (V e) {
                trace("catch(V) di Stage0");
                // ....
                log("esco");
            } catch (IOException e) {
                trace("catch(IOException) di Stage0");
                // ....
                log("esco");
            } finally {
                trace("finally di Stage0");
                // ....
                log("esco");
            }
        }
    }

    public Stage0 stage0 = Stage0.getStage(Stage0.class, this);
    
    // partenza

    public void start() {
        final A a = null;
        // ....
        FirstStage stage = new FirstStage() {
            public void invoke(Chain chain) throws RedirectedException {
                trace("invoke di FirstStage");
                log("lancio stage0");
                chain.setRedirector(new PoolRedirector(myPool));
                stage0.invoke(a, chain);
            }
        };
        ChainOutcomeListener closeListener = new ChainOutcomeListener() {
            public void onClose(Throwable thrown) {
                trace("onClose di ChainOutcomeListener");
                // ....
                if (thrown != null) {
                    thrown.printStackTrace();
                }
            }
        };
        Chain.startChain(stage, closeListener, new MyLogger());
    }
    
    public static void main(String[] args) {
        new Example().start();
    }
    
    // utility:

    private final ThreadPoolExecutor myPool = new ThreadPoolExecutor();
    
    private final AsynchronouSocketChannel mySocket = new AsynchronouSocketChannel();
    
    private static void trace(String text) {
        System.out.print("in ");
        System.out.print(text);
        System.out.println();
    }

    private static void log(String text) {
        System.out.print("   ");
        System.out.print(text);
        System.out.println();
    }
    
    private static class MyLogger extends Logger implements LogConsumer {}

}

