package it.weswit.topdownchain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

public abstract class FullStage extends StageBase {
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface StageBody {
        // legal empty block
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface StageInvocation {
        // legal empty block
    }
    
    public static <STAGE extends FullStage> STAGE getStage(Class<STAGE> stageClass, Object... constrArgs) {
        ProxyInfo info = getProxyInfo(stageClass);
        return getStageInternal(stageClass, info, constrArgs);
    }
    
    private static <STAGE extends FullStage> ProxyInfo getProxyInfo(Class<STAGE> stageClass) {
        
        // 1) check the cache first
        
        ProxyInfo cachedInfo = getCache(stageClass);
        if (cachedInfo != null) {
            return cachedInfo;
        }
        // we are not synchronized, so we may get here multiple times
        // for the same class; hence, to avoid redundant initializations,
        // the following synchronization is still needed;
        // however, this overhead can only happen in the initial phase
        synchronized(stageClass) {
            cachedInfo = getCache(stageClass);
            if (cachedInfo != null) {
                return cachedInfo;
            }
        
            // 2) find the needed methods
            
            Method[] methods = stageClass.getDeclaredMethods();
            Method invoke = findRequiredMethod(StageInvocation.class, methods, "invoke");
            Method body = findRequiredMethod(StageBody.class, methods, "body");
            boolean isVoidInvoke = invoke.getReturnType() == void.class;
            boolean isReturnCompatible;
            if (isVoidInvoke) {
                isReturnCompatible = true; // the return from body is ignored by invoke
            } else if (body.getReturnType() == void.class) {
                isReturnCompatible = true; // the return from body is null, always accepted
            } else if (body.getReturnType() == invoke.getReturnType()) {
                isReturnCompatible = true; // the return from body is accepted by invoke
            } else {
                isReturnCompatible = false; // this will need some extra processing
            }
            
            // 3) check the conditions on the methods
            
            Class<?>[] parms0 = invoke.getParameterTypes();
            Class<?>[] parms1 = body.getParameterTypes();
            if (parms1.length != parms0.length) {
                throw new PatternException("Arguments of body and invoke are different");
            }
            for (int i = 0; i < parms0.length; i++) {
                if (parms0[i] != parms1[i]) {
                    throw new PatternException("Arguments of body and invoke don't match");
                }
            }
    
            // 4) setup a temporary ProxyFactory for this class
            
            ProxyFactory factory = new ProxyFactory();
            factory.setFilter(new MethodFilter() {
                public boolean isHandled(Method m) {
                     return m.isAnnotationPresent(StageBody.class) ||
                             m.isAnnotationPresent(StageInvocation.class) ||
                             m.isAnnotationPresent(AbstractForcer.class);
                }
            });
            factory.setSuperclass(stageClass);
            factory.setUseCache(false);
            Class<STAGE> proxyClass = factory.createClass();
            
            // 5) extract the methods from the proxy class
            
            TempMethodHandler tempHandler = new TempMethodHandler();
            for (Method method : proxyClass.getDeclaredMethods()) {
                if (method.getName().startsWith("_d")) {
                    // the prefix is cast in ProxyFactory code
                    // (note that it is generated at runtime, hence it cannot be obfuscated)
                    tempHandler.delegators.add(method);
                }
            }
            assert(tempHandler.delegators.size() == 2);
            
            // 6) set the proxy
            
            tempHandler.refStageClass = stageClass;
            tempHandler.isReturnCompatible = isReturnCompatible;
            ProxyInfo info = new ProxyInfo(proxyClass, tempHandler, isVoidInvoke);
            tempHandler.tempInfo = info;
            setCache(stageClass, info);
            return info;
        }
    }
    
    private static class TempMethodHandler implements MethodHandler {
        public Class<? extends FullStage> refStageClass;
        public boolean isReturnCompatible;
        public ProxyInfo tempInfo;
        public ArrayList<Method> delegators = new ArrayList<Method>();
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            assert(this == tempInfo.proxyHandler);
            assert(self.getClass() == tempInfo.proxyClass);
            assert(thisMethod.getDeclaringClass() == refStageClass);
                // note: for some reason, the supplied "thisMethod" is different
                // from both the version in the Stage class and the one in the proxy class
            Method bodyDelegator;
            if (thisMethod.isAnnotationPresent(StageInvocation.class)) {
                if (proceed.equals(delegators.get(0))) {
                    bodyDelegator = delegators.get(1);
                } else {
                    assert (proceed.equals(delegators.get(1)));
                    bodyDelegator = delegators.get(0);
                }
            } else {
                assert (thisMethod.isAnnotationPresent(StageBody.class));
                bodyDelegator = proceed;
            }
            FinalMethodHandler finalHandler = new FinalMethodHandler(bodyDelegator, isReturnCompatible);
            ((Proxy) self).setHandler(finalHandler);
            assert (getCache(refStageClass) != null);
            // note that we may assign a TempMethodHandler to multiple objects
            // of the same class before the FinalMethodHandler is set;
            // hence, the above code and the cache replacement below may also
            // be done redundantly;
            // however, this overhead is minimal and can only happen
            // in the initial phase
            setCache(refStageClass, new ProxyInfo(tempInfo.proxyClass, finalHandler, tempInfo.isVoidInvoke));
            return finalHandler.invoke(self, thisMethod, proceed, args);
        }
    }
    
    private static class FinalMethodHandler implements MethodHandler {
        private final Method bodyDelegator;
        private final boolean isReturnCompatible;
        public FinalMethodHandler(Method bodyDelegator, boolean isReturnCompatible) {
            this.bodyDelegator = bodyDelegator;
            this.isReturnCompatible = isReturnCompatible;
        }
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            if (thisMethod.isAnnotationPresent(StageInvocation.class)) {
                try {
                    Chain context = (Chain) args[args.length - 1];
                    // lanciamo body al posto di invoke;
                    // ma non è detto che possiamo gestire il valore di ritorno,
                    // perchè i due tipi di ritorno potrebbero essere incompatibili;
                    // possiamo salvare qualche caso speciale e così ridurre lo stack;
                    // ma è complicato discriminare; gestiamo solo casi semplici
                    if (isReturnCompatible) {
                        return context.launchNext((FullStage) self, bodyDelegator, proceed, args);
                    } else {
                        context.launchNextProtected((FullStage) self, bodyDelegator, proceed, args);
                        return null;
                    }
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                } catch (IllegalAccessException e) {
                    assert(false);
                    throw new PatternException("Unexpected error in body call", e);
                } catch (IllegalArgumentException e) {
                    assert(false);
                    throw e;
                }
            } else {
                assert (thisMethod.isAnnotationPresent(StageBody.class));
                assert (proceed.equals(bodyDelegator));
                Chain context = (Chain) args[args.length - 1];
                return context.playback();
            }
        }
    }

    public Throwable getBodyException(Chain chain) {
        return chain.getBodyException();
    }

    public boolean wasBodySuccessful(Chain chain) {
        return chain.getBodyException() == null;
    }

}