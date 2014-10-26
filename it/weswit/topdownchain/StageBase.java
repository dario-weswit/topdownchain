package it.weswit.topdownchain;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;

public abstract class StageBase {
    
    private boolean isVoidInvoke = false;
    
    public void finallyCheck() {
        if (! isVoidInvoke) {
            throw new PatternException("invoke method is not void");
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface AbstractForcer {
        // legal empty block
    }
    
    @AbstractForcer
    abstract void abstractMethodThatPreventsStageInstantiation();
    
    private static final ConcurrentHashMap<Class<? extends StageBase>, ProxyInfo> cache =
            new ConcurrentHashMap<Class<? extends StageBase>, ProxyInfo>();

    static class ProxyInfo {
        public final Class<? extends StageBase> proxyClass;
        public final MethodHandler proxyHandler;
        public final boolean isVoidInvoke;
        public <STAGE extends StageBase> ProxyInfo(Class<STAGE> proxyClass, MethodHandler proxyHandler, boolean isVoidInvoke) {
            this.proxyClass = proxyClass;
            this.proxyHandler = proxyHandler;
            this.isVoidInvoke = isVoidInvoke;
        }
    }

    static ProxyInfo getCache(Class<? extends StageBase> stageClass) {
        return cache.get(stageClass);
    }

    static void setCache(Class<? extends StageBase> stageClass, ProxyInfo info) {
        cache.put(stageClass, info);
    }

    static <STAGE extends StageBase> STAGE getStageInternal(Class<STAGE> stageClass, ProxyInfo info, Object[] constrArgs) {
        Class<?>[] argsTypes = new Class<?>[constrArgs.length];
        for (int i = 0; i < constrArgs.length; i++) {
            argsTypes[i] = constrArgs[i].getClass();
        }
        Constructor<? extends StageBase> constr;
        try {
            constr = info.proxyClass.getConstructor(argsTypes);
        } catch (NoSuchMethodException e) {
            throw new PatternException("Suitable constructor not found in stage class", e);
        } catch (SecurityException e) {
            throw new PatternException("Cannot find a suitable constructor in stage class", e);
        }
        Object proxy;
        try {
            proxy = constr.newInstance(constrArgs);
        } catch (InstantiationException e) {
            throw new PatternException("Failed to instantiate the stage class", e);
        } catch (IllegalAccessException e) {
            throw new PatternException("Unable to instantiate the stage class", e);
        } catch (IllegalArgumentException e) {
            throw new PatternException("Cannot instantiate the stage class", e);
        } catch (InvocationTargetException e) {
            throw new PatternException("Error while instantiating the stage class", e);
        }
        ((Proxy) proxy).setHandler(info.proxyHandler);
        ((StageBase) proxy).isVoidInvoke = info.isVoidInvoke;
        return (STAGE) proxy;
    }
    
    static Method findRequiredMethod(Class<? extends Annotation> annotation, Method[] methods, String type) {
        
        // 1) find the method

        Method found = null;
        for (Method m : methods) {
            if (m.isAnnotationPresent(annotation)) {
                if (found != null) {
                    throw new PatternException("Duplicate " + type + " annotation");
                }
                found = m;
            }
        }
        if (found == null) {
            throw new PatternException("Missing " + type + " annotation");
        }
        
        // 2) check the conditions on the methods
        
        Class<?>[] parms = found.getParameterTypes();
        if (parms.length == 0 || parms[parms.length - 1] != Chain.class) {
            throw new PatternException("Last argument in " + type + " must be a Chain");
        }
        Class<?>[] exceptions = found.getExceptionTypes();
        boolean ok = false;
        for (Class<?> exc : exceptions) {
            if (exc == RedirectedException.class) {
                ok = true;
            }
        }
        if (! ok) {
            throw new PatternException(type + " must explicitly throw a RedirectedException");
        }
        return found;
    }

    // Currently unneeded
    static void checkConstructors(Class<?> stageClass, Class<?> parentClass) {
        Constructor<?>[] constrs = stageClass.getConstructors();
        if (constrs.length == 0) {
            throw new PatternException("No constructors found in stage class");
        } else if (constrs.length > 1) {
            throw new PatternException("More than one constructor found in stage class");
        }
        Class<?>[] parms = constrs[0].getParameterTypes();
        if (parentClass != null) {
            if (parms.length != 1 || parms[0] != parentClass) {
                throw new PatternException("Expected single argument constructor in stage class");
            }
        } else {
            if (parms.length != 0) {
                throw new PatternException("Expected empty constructor in stage class");
            }
        }
    }

    // currently unneeded
    static boolean overrides(Method toCheck, Method parent) {
        assert(parent.getDeclaringClass().isAssignableFrom(toCheck.getDeclaringClass()));
        if (parent.getName().equals(toCheck.getName())) {
             Class<?>[] params1 = parent.getParameterTypes();
             Class<?>[] params2 = toCheck.getParameterTypes();
             if (params1.length == params2.length) {
                 for (int i = 0; i < params1.length; i++) {
                     if (!params1[i].equals(params2[i])) {
                         return false;
                     }
                 }
                 return true;
             }
        }
        return false;
    }
    
}