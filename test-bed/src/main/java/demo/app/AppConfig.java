package demo.app;

import jakarta.inject.Singleton;
import vest.assist.annotations.Configuration;
import vest.assist.annotations.Factory;
import vest.assist.annotations.Scan;
import vest.assist.conf.ConfigurationFacade;
import vest.assist.jpa.JPAContextBootstrapper;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;

@Configuration
@Scan("demo.app")
@PersistenceContext(
        unitName = "jpa-test",
        synchronization = SynchronizationType.SYNCHRONIZED,
        properties = {
                @PersistenceProperty(name = JPAContextBootstrapper.PRIMARY, value = "true")
        })
public class AppConfig {

    @Factory
    @Singleton
    public ConfigurationFacade configurationFacade() {
        return ConfigurationFacade.build()
                .environment()
                .system()
                .structured("./synthetic.properties")
                .enableCaching()
                .enableEnvironments()
                .enableInterpolation()
                .finish();
    }
}
