package vest.assist.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A general purpose source of configuration property values. Brings together many configuration sources to create an
 * aggregate properties pool where the sources will be polled in order and the first to return a non-null value wins
 * (in a manner of speaking).
 */
public interface ConfigurationFacade extends ConfigurationSource {

    /**
     * Start building a new ConfigurationFacade.
     *
     * @return a new Builder
     */
    static Builder build() {
        return new Builder();
    }

    /**
     * Return a list of the {@link ConfigurationSource}s backing this facade. The order of the list will be
     * the order that the sources are polled for values.
     *
     * @return A list of ConfigurationSources
     */
    List<ConfigurationSource> sources();

    /**
     * Get a property from the backing configuration sources.
     *
     * @param propertyName the property name
     * @return A property value from the first configuration source that has a value for it, or null if it is not found
     * in any of the sources
     */
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

    /**
     * Get a trace of which ConfigurationSource will provide the value for the property. For a facade that
     * has many sources this can be useful to see which source a property is actually coming from.
     * The format of the returned string is implementation dependent, and, as such, is meant to be informative only.
     *
     * @param propertyName The name of the property to trace
     * @return a list of trace strings, one per property source indicating whether it would return a value for
     * the given property.
     */
    default List<String> trace(String propertyName) {
        List<String> trace = new LinkedList<>();
        boolean found = false;
        for (ConfigurationSource source : sources()) {
            String value = source.get(propertyName);
            if (value == null || value.isEmpty()) {
                trace.add("MISS: [" + source + "]");
            } else if (!found) {
                found = true;
                trace.add("HIT: [" + source + "] will provide the property value: " + value);
            } else {
                trace.add("HIT: [" + source + "] has a value for the property, but it will not be used");
            }
        }
        return trace;
    }

    /**
     * Get a property value or the fallback value if it is not found.
     *
     * @param propertyName the property name
     * @param fallback     the fallback value if the property is not found
     * @return The property value or the fallback if it's not found
     */
    default String get(String propertyName, String fallback) {
        String value = get(propertyName);
        return value != null ? value : fallback;
    }

    /**
     * Get a property value, converted using the given mapped, or the fallback value if the property is not found,
     *
     * @param propertyName the property name
     * @param fallback     the fallback value
     * @param mapper       the converter that turns the string value into the expected type
     * @return The converted property value, or the fallback if it is not found
     */
    default <T> T get(String propertyName, T fallback, Function<String, T> mapper) {
        return Optional.ofNullable(get(propertyName))
                .map(mapper)
                .orElse(fallback);
    }

    /**
     * Get a property value, converted into the given type. The facade will automatically look up the converter for the
     * target type. Any class that has a static valueOf (like {@link Integer#valueOf(int)}), forString, or forName method,
     * or a constructor that takes a single String argument is eligible for automatic conversion.
     *
     * @param propertyName the property name
     * @param type         the target type for the value
     * @return the converted property value, or null if it is not found
     */
    default <T> T get(String propertyName, Class<T> type) {
        return get(propertyName, null, type);
    }

    /**
     * Get a property value, converted into the given type, or the fallback if none is found. The facade will automatically look up the converter for the
     * target type. Any class that has a static valueOf (like {@link Integer#valueOf(int)}), forString, or forName method,
     * or a constructor that takes a single String argument is eligible for automatic conversion.
     *
     * @param propertyName the property name
     * @param fallback     the fallback value
     * @param type         the target type for the value
     * @return the converted property value, or the fallback if it is not found
     */
    <T> T get(String propertyName, T fallback, Class<T> type);

    /**
     * Get a property value, interpreted as a comma delimited list.
     *
     * @param propertyName the property name
     * @return A list of values, or an empty list if the property is not found
     */
    default List<String> getList(String propertyName) {
        return getList(propertyName, String.class);
    }

    /**
     * Get a property value, interpreted as a comma delimited list. Or use the fallback list
     * if the property is not found.
     *
     * @param propertyName the property name
     * @param fallback     the fallback value
     * @return A list of values, or the fallback if the property is not found
     */
    default List<String> getList(String propertyName, List<String> fallback) {
        return getList(propertyName, String.class, fallback);
    }

    /**
     * Get a property value, interpreted as a comma delimited set.
     *
     * @param propertyName the property name
     * @return A set of values, or an empty set if the property is not found
     */
    default Set<String> getSet(String propertyName) {
        return getSet(propertyName, String.class);
    }

    /**
     * Get a property value, interpreted as a comma delimited set. Or use the fallback set
     * if the property is not found.
     *
     * @param propertyName the property name
     * @param fallback     the fallback value
     * @return A set of values, or the fallback if the property is not found
     */
    default Set<String> getSet(String propertyName, Set<String> fallback) {
        return getSet(propertyName, String.class, fallback);
    }

    /**
     * Get a property value, interpreted as a comma delimited list of values converted to the given type.
     *
     * @param propertyName the property name
     * @param genericType  the target type of the values
     * @return A list of values, converted ot the target type, or an empty list if the property is not found
     */
    default <T> List<T> getList(String propertyName, Class<T> genericType) {
        return getList(propertyName, genericType, Collections.emptyList());
    }

    /**
     * Get a property value, interpreted as a comma delimited list of values converted to the given type. Or use the
     * fallback list if the property is not found.
     *
     * @param propertyName the property name
     * @param genericType  the target type of the values
     * @param fallback     the fallback value
     * @return A list of values, or the fallback if the property is not found
     */
    default <T> List<T> getList(String propertyName, Class<T> genericType, List<T> fallback) {
        return getStream(propertyName, genericType, fallback.stream()).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get a property value, interpreted as a comma delimited set of values converted to the given type.
     *
     * @param propertyName the property name
     * @param genericType  the target type of the values
     * @return A set of values
     */
    default <T> Set<T> getSet(String propertyName, Class<T> genericType) {
        return getSet(propertyName, genericType, Collections.emptySet());
    }

    /**
     * Get a property value, interpreted as a comma delimited set of values converted to the given type. Or use the
     * fallback set if the property is not found.
     *
     * @param propertyName the property name
     * @param genericType  the target type of the values
     * @param fallback     the fallback value
     * @return A set of value, or the fallback if the property is not found
     */
    default <T> Set<T> getSet(String propertyName, Class<T> genericType, Set<T> fallback) {
        return getStream(propertyName, genericType, fallback.stream()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Get a property value, interpreted as a comma delimited stream, converted to the target type, and stream to the
     * caller.
     *
     * @param propertyName the property name
     * @param genericType  the target type for the values
     * @return the stream of converted values, or the fallback stream if the property is not found
     */
    default <T> Stream<T> getStream(String propertyName, Class<T> genericType) {
        return getStream(propertyName, genericType, Stream.empty());
    }

    /**
     * Get a property value, interpreted as a comma delimited stream, converted to the target type, and streamed to the
     * caller.
     *
     * @param propertyName the property name
     * @param genericType  the target type for the values
     * @param fallback     the fallback stream if the property is not found
     * @return the stream of converted values, or the fallback stream if the property is not found
     */
    <T> Stream<T> getStream(String propertyName, Class<T> genericType, Stream<T> fallback);

    /**
     * Turn this facade into a Properties object. The returned Properties object should be considered read-only as support
     * for modification will be implementation specific.
     *
     * @return a properties object that has access to all the properties of this facade
     */
    default Properties toProperties() {
        return new FacadeToProperties(this);
    }
}
