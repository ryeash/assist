package vest.assist.conf;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link ConfigurationFacade}s. The order of the method calls is important as that will be the order the
 * configuration sources will be checked for property values.
 */
public class Builder {

    /**
     * Build a ConfigurationFacade that will get properties from (in order): environment variables, system properties,
     * and the given properties file, as well as support macros (using the syntax '${otherProperty}') and environment
     * switches.
     *
     * @param fileProperties The location of the properties file to load
     * @return A new ConfigurationFacade
     */
    public static ConfigurationFacade defaultFacade(String fileProperties) {
        return start()
                .environment()
                .system()
                .file(fileProperties)
                .enableMacros()
                .enableEnvironments()
                .finish();
    }

    /**
     * Start creating a new ConfigurationFacade. It is preferred to use {@link ConfigurationFacade#build()}.
     *
     * @return A new Builder
     */
    public static Builder start() {
        return new Builder();
    }

    private final List<ConfigurationSource> sources = new ArrayList<>(3);
    private boolean caching = false;
    private boolean environment = false;
    private boolean macro = false;
    private String macroOpen;
    private String macroClose;

    /**
     * Add a system properties source that will get values using {@link System#getProperty(String)}.
     *
     * @return this builder
     */
    public Builder system() {
        return add(new SystemProperties());
    }

    /**
     * Add an environment variable configuration source that will get values using {@link System#getenv(String)}.
     *
     * @return this builder
     */
    public Builder environment() {
        return add(new EnvironmentVariables());
    }

    /**
     * Add a properties file configuration source that will get values by loading a file from the file system into
     * a {@link java.util.Properties} object.
     *
     * @param fileLocation The location of the properties file
     * @return this builder
     */
    public Builder file(String fileLocation) {
        return file(new File(fileLocation));
    }

    /**
     * Add a properties file configuration source that will get values by loading a file from the file system into
     * a {@link java.util.Properties} object.
     *
     * @param propertiesFile The properties file
     * @return this builder
     */
    public Builder file(File propertiesFile) {
        return add(new PropertiesSource(propertiesFile));
    }

    /**
     * Add a properties file configuration source that will get values by loading a file from the classpath.
     *
     * @param fileLocation The location on the classpath of the properties file
     * @return this builder
     */
    public Builder classpathFile(String fileLocation) {
        URL url = ClassLoader.getSystemResource(fileLocation);
        return add(new PropertiesSource(url));
    }

    /**
     * Add a map as a configuration source.
     *
     * @param map The map to get property values from
     * @return this builder
     */
    public Builder map(Map<String, String> map) {
        return add(new MapSource(map));
    }

    /**
     * Enable caching of property values pulled from the ConfigurationFacade.
     *
     * @return this builder.
     * @see CachingFacade
     */
    public Builder enableCaching() {
        this.caching = true;
        return this;
    }

    /**
     * Enable environment specific properties.
     *
     * @return this builder
     * @see EnvironmentFacade
     */
    public Builder enableEnvironments() {
        this.environment = true;
        return this;
    }

    /**
     * Enable macro support using the default macro enclosures '${' and '}'.
     *
     * @return this builder
     * @see MacroSupportFacade
     */
    public Builder enableMacros() {
        return enableMacros("${", "}");
    }

    /**
     * Enable macro support.
     *
     * @param macroOpen  the open for a macro
     * @param macroClose the close for a macro
     * @return this builder
     * @see MacroSupportFacade
     */
    public Builder enableMacros(String macroOpen, String macroClose) {
        this.macro = true;
        this.macroOpen = macroOpen;
        this.macroClose = macroClose;
        return this;
    }

    /**
     * Add a {@link ConfigurationSource} to the list of sources that will be used by the finished ConfigurationFacade
     * to look up property values. Sources will be polled in the order that they are added.
     *
     * @param source the source to add
     * @return this builder.
     */
    public Builder add(ConfigurationSource source) {
        sources.add(source);
        return this;
    }

    /**
     * Finish building the ConfigurationFacade and return it.
     *
     * @return the ConfigurationFacde
     */
    public ConfigurationFacade finish() {
        ConfigurationFacade facade = new DefaultConfigurationFacade(sources);
        if (environment) {
            facade = new EnvironmentFacade(facade);
        }
        if (macro) {
            facade = new MacroSupportFacade(facade, macroOpen, macroClose);
        }
        if (caching) {
            facade = new CachingFacade(facade);
        }
        return facade;
    }

    public static final class SystemProperties implements ConfigurationSource {
        @Override
        public String get(String propertyName) {
            return System.getProperty(propertyName);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    public class EnvironmentVariables implements ConfigurationSource {
        @Override
        public String get(String propertyName) {
            return System.getenv(propertyName);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    public class MapSource implements ConfigurationSource {

        private final Map<String, String> map;

        public MapSource(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public String get(String propertyName) {
            return map.get(propertyName);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + map.getClass().getSimpleName() + ":" + map.hashCode() + ")";
        }
    }
}
