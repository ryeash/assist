package vest.assist.conf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wrapper around a ConfigurationFacade that caches the result of the {@link ConfigurationFacade#get(String)}
 * method. In cases where property polling is not a negligible in-memory lookup (e.g. a network call of some kind)
 * this class can be used to speed up property wiring. Internally uses a {@link ConcurrentHashMap} to store the values.
 */
public class CachingFacade extends ConfigurationFacadeWrapper {

    private final Map<String, String> cache;

    public CachingFacade(ConfigurationFacade delegate) {
        super(delegate);
        this.cache = new ConcurrentHashMap<>(256, .9F, 4);
    }

    @Override
    public String get(String propertyName) {
        return cache.computeIfAbsent(propertyName, delegate::get);
    }

    @Override
    public void reload() {
        super.reload();
        cache.clear();
    }
}
