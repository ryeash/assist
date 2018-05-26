package vest.assist.conf;

import java.util.Properties;

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
}
