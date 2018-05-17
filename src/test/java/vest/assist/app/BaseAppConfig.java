package vest.assist.app;

import vest.assist.annotations.Factory;

import javax.inject.Singleton;
import java.util.Properties;

public class BaseAppConfig {

    final Properties properties;

    public BaseAppConfig() {
        properties = new Properties();
    }

    @Factory
    @Singleton
    public Properties propertiesFactory() {
        return properties;
    }
}
