package vest.assist.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ConfigurationFacade extends ConfigurationSource {

    static Builder build() {
        return new Builder();
    }

    List<ConfigurationSource> sources();

    @Override
    default String get(String propertyName) {
        for (ConfigurationSource source : sources()) {
            String value = source.get(propertyName);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    default String get(String propertyName, String fallback) {
        String value = get(propertyName);
        return value != null ? value : fallback;
    }

    default <T> T get(String propertyName, T fallback, Function<String, T> mapper) {
        return Optional.ofNullable(get(propertyName))
                .map(mapper)
                .orElse(fallback);
    }

    default <T> T get(String propertyName, Class<T> type) {
        return get(propertyName, null, type);
    }

    <T> T get(String propertyName, T fallback, Class<T> type);

    default List<String> getList(String propertyName) {
        return getList(propertyName, String.class);
    }

    default List<String> getList(String propertyName, List<String> fallback) {
        return getList(propertyName, String.class, fallback);
    }

    default Set<String> getSet(String propertyName) {
        return getSet(propertyName, String.class);
    }

    default Set<String> getSet(String propertyName, Set<String> fallback) {
        return getSet(propertyName, String.class, fallback);
    }

    default <T> List<T> getList(String propertyName, Class<T> genericType) {
        return getList(propertyName, genericType, Collections.emptyList());
    }

    default <T> List<T> getList(String propertyName, Class<T> genericType, List<T> fallback) {
        return getStream(propertyName, genericType, fallback.stream()).collect(Collectors.toCollection(ArrayList::new));
    }

    default <T> Set<T> getSet(String propertyName, Class<T> genericType) {
        return getSet(propertyName, genericType, Collections.emptySet());
    }

    default <T> Set<T> getSet(String propertyName, Class<T> genericType, Set<T> fallback) {
        return getStream(propertyName, genericType, fallback.stream()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default <T> Stream<T> getStream(String propertyName, Class<T> genericType) {
        return getStream(propertyName, genericType, Stream.empty());
    }

    <T> Stream<T> getStream(String propertyName, Class<T> genericType, Stream<T> fallback);

    default Properties toProperties() {
        return new FacadeToProperties(this);
    }
}
