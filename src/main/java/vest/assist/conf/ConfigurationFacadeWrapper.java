package vest.assist.conf;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A wrapper that delegates all method calls to the facade used in the constructor.
 */
public class ConfigurationFacadeWrapper implements ConfigurationFacade {

    protected final ConfigurationFacade delegate;

    public ConfigurationFacadeWrapper(ConfigurationFacade delegate) {
        this.delegate = delegate;
    }

    public static Builder build() {
        return ConfigurationFacade.build();
    }

    @Override
    public List<ConfigurationSource> sources() {
        return delegate.sources();
    }

    @Override
    public String get(String propertyName) {
        return delegate.get(propertyName);
    }

    @Override
    public List<String> trace(String propertyName) {
        return delegate.trace(propertyName);
    }

    @Override
    public String get(String propertyName, String fallback) {
        return delegate.get(propertyName, fallback);
    }

    @Override
    public <T> T get(String propertyName, T fallback, Function<String, T> mapper) {
        return delegate.get(propertyName, fallback, mapper);
    }

    @Override
    public <T> T get(String propertyName, Class<T> type) {
        return delegate.get(propertyName, type);
    }

    @Override
    public <T> T get(String propertyName, T fallback, Class<T> type) {
        return delegate.get(propertyName, fallback, type);
    }

    @Override
    public List<String> getList(String propertyName) {
        return delegate.getList(propertyName);
    }

    @Override
    public List<String> getList(String propertyName, List<String> fallback) {
        return delegate.getList(propertyName, fallback);
    }

    @Override
    public Set<String> getSet(String propertyName) {
        return delegate.getSet(propertyName);
    }

    @Override
    public Set<String> getSet(String propertyName, Set<String> fallback) {
        return delegate.getSet(propertyName, fallback);
    }

    @Override
    public <T> List<T> getList(String propertyName, Class<T> genericType) {
        return delegate.getList(propertyName, genericType);
    }

    @Override
    public <T> List<T> getList(String propertyName, Class<T> genericType, List<T> fallback) {
        return delegate.getList(propertyName, genericType, fallback);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Class<T> genericType) {
        return delegate.getSet(propertyName, genericType);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Class<T> genericType, Set<T> fallback) {
        return delegate.getSet(propertyName, genericType, fallback);
    }

    @Override
    public <T> Stream<T> getStream(String propertyName, Class<T> genericType) {
        return delegate.getStream(propertyName, genericType);
    }

    @Override
    public <T> Stream<T> getStream(String propertyName, Class<T> genericType, Stream<T> fallback) {
        return delegate.getStream(propertyName, genericType, fallback);
    }

    @Override
    public Properties toProperties() {
        return delegate.toProperties();
    }

    @Override
    public void reload() {
        delegate.reload();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + delegate + ")";
    }
}
