package vest.assist.jpa.app;

import vest.assist.annotations.Configuration;
import vest.assist.jpa.JPAExtension;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;

@Configuration
@PersistenceContext(
        unitName = "jpa-test",
        synchronization = SynchronizationType.SYNCHRONIZED,
        properties = {
                @PersistenceProperty(name = JPAExtension.PRIMARY, value = "true")
        })
public class JpaAppConfig {
}
