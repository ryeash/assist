package vest.assist.conf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingFacade extends ConfigurationFacadeWrapper {

    private final Map<String, String> cache;

    public CachingFacade(ConfigurationFacade delegate) {
        super(delegate);
        this.cache = new ConcurrentHashMap<>(128, .9F, 2);
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
