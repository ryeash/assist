package vest.assist.jpa.app;

import vest.assist.annotations.Configuration;
import vest.assist.jpa.JPAContextBootstrapper;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;

@Configuration
@PersistenceContext(
        unitName = "jpa-test",
        synchronization = SynchronizationType.SYNCHRONIZED,
        properties = {
                @PersistenceProperty(name = JPAContextBootstrapper.PRIMARY, value = "true")
        })
public class JpaAppConfig {
}
