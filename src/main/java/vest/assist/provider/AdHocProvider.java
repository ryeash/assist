package vest.assist.provider;

import vest.assist.AssistProvider;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * A provider that just returns an instance of an object.
 */
public class AdHocProvider<T> implements AssistProvider<T> {

    private final Class<T> advertisedType;
    private final T instance;
    private final Annotation qualifier;

    public AdHocProvider(Class<T> advertisedType, Annotation qualifier, T instance) {
        this.advertisedType = advertisedType;
        this.instance = instance;
        this.qualifier = qualifier;
    }

    @Override
    public Class<T> type() {
        return advertisedType;
    }

    @Override
    public Annotation qualifier() {
        return qualifier;
    }

    @Override
    public Annotation scope() {
        return null;
    }

    @Override
    public List<Annotation> annotations() {
        return Collections.emptyList();
    }

    @Override
    public T get() {
        return instance;
    }

    @Override
    public String toString() {
        return "AdHocProvider{" + (qualifier != null ? qualifier : "") + instance.getClass().getSimpleName() + "}:" + hashCode();
    }
}
