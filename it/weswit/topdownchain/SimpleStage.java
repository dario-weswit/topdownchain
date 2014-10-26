package it.weswit.topdownchain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public abstract class SimpleStage extends StageBase {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface StageInvocation {
        // legal empty block
    }

    public static <STAGE extends SimpleStage> STAGE getStage(Class<STAGE> stageClass, Object... constrArgs) {
        ProxyInfo info = getProxyInfo(stageClass);
        return getStageInternal(stageClass, info, constrArgs);
    }
    
    private static <STAGE extends SimpleStage> ProxyInfo getProxyInfo(Class<STAGE> stageClass) {
        
        // 1) check the cache first
        
        ProxyInfo cachedInfo = getCache(stageClass);
        if (cachedInfo != null) {
            return cachedInfo;
        }
        // we are not synchronized, so we may get here multiple times
        // for the same class; hence, to avoid redundant initializations,
        // the following synchronization is still needed;
        // however, this overhead can only happen in the initial phase
        synchronized (stageClass) {
            cachedInfo = getCache(stageClass);
            if (cachedInfo != null) {
                return cachedInfo;
            }
            
            // 2) find the needed methods
            
            Method[] methods = stageClass.getDeclaredMethods();
            Method invoke = findRequiredMethod(StageInvocation.class, methods, "invoke");
            boolean isVoidInvoke = invoke.getReturnType() == void.class;
            
            // 3) setup a temporary ProxyFactory for this class
            
            ProxyFactory factory = new ProxyFactory();
            factory.setFilter(new MethodFilter() {
                public boolean isHandled(Method m) {
                     return m.isAnnotationPresent(StageInvocation.class) ||
                             m.isAnnotationPresent(AbstractForcer.class);
                }
            });
            factory.setSuperclass(stageClass);
            factory.setUseCache(false);
            Class<STAGE> proxyClass = factory.createClass();
            
            // 4) set the proxy
            
            ProxyInfo info = new ProxyInfo(proxyClass, new SimpleMethodHandler(), isVoidInvoke);
            setCache(stageClass, info);
            return info;
        }
    }
    
    private static class SimpleMethodHandler implements MethodHandler {
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            assert (thisMethod.isAnnotationPresent(StageInvocation.class));
            try {
                Chain context = (Chain) args[args.length - 1];
                return context.launchNext((SimpleStage) self, proceed, null, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (IllegalAccessException e) {
                assert(false);
                throw new PatternException("Unexpected error in invoke call", e);
            } catch (IllegalArgumentException e) {
                assert(false);
                throw e;
            }
        }
    }

}