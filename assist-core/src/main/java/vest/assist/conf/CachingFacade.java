package vest.assist.conf;

import vest.assist.aop.Aspect;
import vest.assist.aop.AspectInvocationHandler;
import vest.assist.aop.Invocation;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wrapper around a ConfigurationFacade that caches the result of the {@link ConfigurationFacade}
 * methods. In cases where property polling is not a negligible in-memory lookup (e.g. a network call)
 * this class can be used to speed up property wiring. Internally uses a {@link ConcurrentHashMap} to store the values.
 */
public class CachingFacade implements Aspect {

    public static ConfigurationFacade wrap(ConfigurationFacade facade) {
        AspectInvocationHandler aih = new AspectInvocationHandler(facade, new CachingFacade(facade));
        return (ConfigurationFacade) Proxy.newProxyInstance(ConfigurationFacade.class.getClassLoader(),
                new Class[]{ConfigurationFacade.class},
                aih);
    }

    private final Map<Invocation, Object> cache;
    private final ConfigurationFacade facade;

    public CachingFacade(ConfigurationFacade delegate) {
        this.facade = delegate;
        this.cache = new ConcurrentHashMap<>(256, .9F, 4);
    }

    private Object invokeMethod(Invocation invocation) {
        try {
            return invocation.next();
        } catch (Throwable e) {
            throw new RuntimeException("error executing proxy'd method", e);
        }
    }

    @Override
    public Object invoke(Invocation invocation) throws Exception {
        if (invocation.method().getName().startsWith("get") && !invocation.method().getName().equals("getStream")) {
            return cache.computeIfAbsent(invocation, this::invokeMethod);
        }
        switch (invocation.method().getName()) {
            case "reload":
                cache.clear();
                facade.reload();
                return null;
            case "toString":
                return "CachingFacade(" + facade + ")";
            default:
                return invocation.next();
        }
    }
}
