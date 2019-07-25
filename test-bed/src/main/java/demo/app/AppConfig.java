package demo.app;

import vest.assist.annotations.Configuration;
import vest.assist.annotations.Scan;
import vest.assist.jpa.JPAExtension;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;

@Configuration
@Scan("demo.app")
@PersistenceContext(
        unitName = "jpa-test",
        synchronization = SynchronizationType.SYNCHRONIZED,
        properties = {
                @PersistenceProperty(name = JPAExtension.PRIMARY, value = "true")
        })
public class AppConfig {
}
