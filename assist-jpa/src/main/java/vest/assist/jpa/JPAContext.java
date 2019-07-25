package vest.assist.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class JPAContext {

    private final PersistenceContext persistenceContext;
    private final Map<String, String> properties;
    private final EntityManagerFactory entityManagerFactory;
    private final ThreadLocal<EntityManager> threadLocalEntityManager;

    public JPAContext(PersistenceContext persistenceContext, EntityManagerFactory entityManagerFactory) {
        this.persistenceContext = persistenceContext;
        this.properties = new HashMap<>((int) Math.ceil(persistenceContext.properties().length / 8) * 8);
        for (PersistenceProperty property : persistenceContext.properties()) {
            properties.put(property.name(), property.value());
        }
        this.entityManagerFactory = entityManagerFactory;
        this.threadLocalEntityManager = ThreadLocal.withInitial(this::createEntityManager);
    }

    private EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager(persistenceContext.synchronization(), properties);
    }

    public EntityManagerFactory entityManagerFactory() {
        return entityManagerFactory;
    }

    public void managed(Consumer<EntityManager> consumer) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            consumer.accept(entityManager);
        } finally {
            entityManager.close();
        }
    }

    public <T> T managed(Function<EntityManager, T> function) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return function.apply(entityManager);
        } finally {
            entityManager.close();
        }
    }

    public void inTransaction(Consumer<EntityManager> consumer) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            consumer.accept(entityManager);
            entityManager.getTransaction().commit();
        } catch (Throwable t) {
            entityManager.getTransaction().rollback();
            throw t;
        } finally {
            entityManager.close();
        }
    }

    public <T> T inTransaction(Function<EntityManager, T> function) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            T result = function.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (Throwable t) {
            entityManager.getTransaction().rollback();
            throw t;
        } finally {
            entityManager.close();
        }
    }

    public EntityManager local() {
        return threadLocalEntityManager.get();
    }

    public void closeLocal() {
        EntityManager entityManager = threadLocalEntityManager.get();
        threadLocalEntityManager.remove();
        entityManager.close();
    }
}
