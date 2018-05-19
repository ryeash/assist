package vest.assist.conf;

@FunctionalInterface
public interface ConfigurationSource {

    String get(String propertyName);

    default void reload(){
        // no-op
    }
}
