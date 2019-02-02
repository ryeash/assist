package vest.assist.conf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wrapper around a ConfigurationFacade that caches the result of the {@link ConfigurationFacade}
 * methods. In cases where property polling is not a negligible in-memory lookup (e.g. a network call)
 * this class can be used to speed up property wiring. Internally uses a {@link ConcurrentHashMap} to store the values.
 */
public class CachingFacade implements InvocationHandler {

    public static ConfigurationFacade wrap(ConfigurationFacade facade) {
        return (ConfigurationFacade) Proxy.newProxyInstance(ConfigurationFacade.class.getClassLoader(),
                new Class[]{ConfigurationFacade.class},
                new CachingFacade(facade));
    }

    private final Map<CacheKey, Object> cache;
    private final ConfigurationFacade facade;

    public CachingFacade(ConfigurationFacade delegate) {
        this.facade = delegate;
        this.cache = new ConcurrentHashMap<>(256, .9F, 4);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        CacheKey key = new CacheKey(method, args);
        if (method.getName().startsWith("get")) {
            return cache.computeIfAbsent(key, this::invokeMethod);
        }
        switch (method.getName()) {
            case "reload":
                cache.clear();
                facade.reload();
                return null;
            case "toString":
                return "CachingFacade(" + facade + ")";
            default:
                return invokeMethod(method, args);
        }
    }

    private Object invokeMethod(CacheKey key) {
        return invokeMethod(key.method, key.parameters);
    }

    private Object invokeMethod(Method method, Object[] args) {
        try {
            return method.invoke(facade, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("error executing proxy'd method", e);
        }
    }

    public static class CacheKey {
        private final Method method;
        private final Object[] parameters;
        private final int hash;

        public CacheKey(Method method, Object... parameters) {
            this.method = method;
            this.parameters = parameters;
            int temp = method.getName().hashCode();
            if (parameters != null) {
                for (Object parameter : parameters) {
                    if (parameter != null) {
                        temp += 31 * (parameter instanceof Class ? ((Class) parameter).getName().hashCode() : parameter.hashCode());
                    }
                }
            }
            this.hash = temp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(method.getName(), cacheKey.method.getName()) &&
                    Arrays.equals(parameters, cacheKey.parameters);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
