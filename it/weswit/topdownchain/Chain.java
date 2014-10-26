package it.weswit.topdownchain;

import javassist.util.proxy.ProxyFactory;

public class Chain extends ChainInternal {
    
    private Chain(LogConsumer logger) {
        super(logger);
    }

    public void setRedirector(BaseRedirector manager) {
        super.setRedirector(manager);
    }
    
    public <E1 extends Exception>
        void setRedirector(BaseRedirector.With1<E1> manager)
            throws E1
            // non potendo aggiungerle alla invoke del proxy, le dichiariamo qui
    {
        super.setRedirector(manager);
    }

    public <E1 extends Exception, E2 extends Exception>
        void setRedirector(BaseRedirector.With2<E1, E2> manager)
            throws E1, E2
            // non potendo aggiungerle alla invoke del proxy, le dichiariamo qui
    {
        super.setRedirector(manager);
    }
    
    public <E1 extends Exception, E2 extends Exception, E3 extends Exception>
        void setRedirector(BaseRedirector.With3<E1, E2, E3> manager)
            throws E1, E2, E3
            // non potendo aggiungerle alla invoke del proxy, le dichiariamo qui
    {
        super.setRedirector(manager);
    }
    
    public <E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception>
        void setRedirector(BaseRedirector.With4<E1, E2, E3, E4> manager)
            throws E1, E2, E3, E4
            // non potendo aggiungerle alla invoke del proxy, le dichiariamo qui
    {
        super.setRedirector(manager);
    }

    public void redirectAndClose(BaseRedirector manager)
            throws RedirectedException
    {
        setRedirector(manager);
        emptyStage.invoke(this);
    }

    public <E1 extends Exception>
        void redirectAndClose(BaseRedirector.With1<E1> manager)
            throws E1, RedirectedException
    {
        setRedirector(manager);
        emptyStage.invoke(this);
    }
    
    public <E1 extends Exception, E2 extends Exception>
        void redirectAndClose(BaseRedirector.With2<E1, E2> manager)
            throws E1, E2, RedirectedException
    {
        setRedirector(manager);
        emptyStage.invoke(this);
    }
    
    public <E1 extends Exception, E2 extends Exception, E3 extends Exception>
        void redirectAndClose(BaseRedirector.With3<E1, E2, E3> manager)
            throws E1, E2, E3, RedirectedException
    {
        setRedirector(manager);
        emptyStage.invoke(this);
    }
    
    public <E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception>
        void redirectAndClose(BaseRedirector.With4<E1, E2, E3, E4> manager)
            throws E1, E2, E3, E4, RedirectedException
    {
        setRedirector(manager);
        emptyStage.invoke(this);
    }

    public <RET> RET redirectAndReturn(BaseRedirector manager, RET ret)
            throws RedirectedException
    {
        setRedirector(manager);
        return (RET) echoStage.invoke(ret, this);
    }

    public <RET, E1 extends Exception>
        RET redirectAndReturn(BaseRedirector.With1<E1> manager, RET ret)
            throws E1, RedirectedException
    {
        setRedirector(manager);
        return (RET) echoStage.invoke(ret, this);
    }
    
    public <RET, E1 extends Exception, E2 extends Exception>
        RET redirectAndReturn(BaseRedirector.With2<E1, E2> manager, RET ret)
            throws E1, E2, RedirectedException
    {
        setRedirector(manager);
        return (RET) echoStage.invoke(ret, this);
    }
    
    public <RET, E1 extends Exception, E2 extends Exception, E3 extends Exception>
        RET redirectAndReturn(BaseRedirector.With3<E1, E2, E3> manager, RET ret)
            throws E1, E2, E3, RedirectedException
    {
        setRedirector(manager);
        return (RET) echoStage.invoke(ret, this);
    }
    
    public <RET, E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception>
        RET redirectAndReturn(BaseRedirector.With4<E1, E2, E3, E4> manager, RET ret)
            throws E1, E2, E3, E4, RedirectedException
    {
        setRedirector(manager);
        return (RET) echoStage.invoke(ret, this);
    }

    @Override
    public void addClosingHook(ChainOutcomeListener hook) {
        super.addClosingHook(hook);
    }
    
    public static void startChain(FirstStage stage, ChainOutcomeListener closeListener, LogConsumer logger) {
        Chain chain = new Chain(logger);
        chain.start(stage, closeListener);
    }
    
    
    public static abstract class EmptyStage extends SimpleStage {
        @StageInvocation
        public void invoke(Chain chain) throws RedirectedException {}
    }
    
    private static EmptyStage emptyStage = EmptyStage.getStage(EmptyStage.class);
    
    public static abstract class EchoStage extends SimpleStage {
        @StageInvocation
        public Object invoke(Object ret, Chain chain) throws RedirectedException {
            return ret;
        }
    }
    
    private static EchoStage echoStage = EchoStage.getStage(EchoStage.class);
    
    // currently unneeded
    public static void forceContextClassLoaderForJavassist() {
        ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
            public ClassLoader get(ProxyFactory pf) {
                return Thread.currentThread().getContextClassLoader();
            }
        };
    }
    
}

