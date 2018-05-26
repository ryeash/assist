package vest.assist.conf;

/**
 * An interface for any object that can take a property name and retrieves it's value.
 */
@FunctionalInterface
public interface ConfigurationSource {

    /**
     * Get the property value.
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
