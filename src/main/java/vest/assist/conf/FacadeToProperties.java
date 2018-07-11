package vest.assist.conf;

import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Used internally by {@link ConfigurationFacade#toProperties()} to create a {@link Properties} compatible object
 * out of a configuration facade.
 */
public class FacadeToProperties extends Properties {

    private final ConfigurationFacade facade;

    public FacadeToProperties(ConfigurationFacade facade) {
        this.facade = facade;
    }

    @Override
    public Object get(Object key) {
        return facade.get(String.valueOf(key));
    }

    @Override
    public String getProperty(String key) {
        return facade.get(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return facade.get(key, defaultValue);
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean contains(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object getOrDefault(Object key, Object defaultValue) {
        return facade.get(String.valueOf(key), String.valueOf(defaultValue));
    }

    @Override
    public synchronized Object putIfAbsent(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object replace(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new UnsupportedOperationException();
    }
}
