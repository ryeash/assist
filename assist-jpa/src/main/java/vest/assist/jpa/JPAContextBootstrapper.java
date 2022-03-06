package vest.assist.jpa;

import vest.assist.Assist;
import vest.assist.AssistContextBootstrapper;
import vest.assist.ConfigurationProcessor;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import java.util.HashMap;
import java.util.Map;

public class JPAContextBootstrapper implements AssistContextBootstrapper, ConfigurationProcessor {

    public static final String PRIMARY = "primary";

    @Override
    public void load(Assist assist) {
        assist.register(this);
    }

    @Override
    public void process(Object configuration, Assist assist) {
        PersistenceContext jpa = configuration.getClass().getAnnotation(PersistenceContext.class);
        if (jpa != null) {
            if (jpa.unitName().isEmpty()) {
                throw new IllegalArgumentException("the persistence unit name must be specified");
            }
            Map<String, String> properties = new HashMap<>();
            for (PersistenceProperty property : jpa.properties()) {
                properties.put(property.name(), property.value());
            }
            EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(jpa.unitName(), properties);
            JPAContext JPAContext = new JPAContext(jpa, entityManagerFactory);
            assist.setSingleton(JPAContext.class, jpa.unitName(), JPAContext);
            if (Boolean.valueOf(properties.get(PRIMARY))) {
                assist.setSingleton(JPAContext.class, JPAContext);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
