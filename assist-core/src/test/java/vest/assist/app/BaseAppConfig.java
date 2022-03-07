package vest.assist.app;

import jakarta.inject.Singleton;
import vest.assist.annotations.Factory;

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
