package vest.assist.conf;

/**
 * Defines an object that can take a property name and retrieve it's value.
 */
@FunctionalInterface
public interface ConfigurationSource {

    /**
     * Get a property value.
     *
     * @param propertyName the property name
     * @return the property value
     */
    String get(String propertyName);

    /**
     * Reload this source. What this does is entirely up to the implementation. By default, it does nothing.
     */
    default void reload() {
        // no-op
    }
}
