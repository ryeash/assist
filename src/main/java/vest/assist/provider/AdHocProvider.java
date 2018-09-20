package vest.assist.provider;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * A provider that just returns an instance of an object
 */
public class AdHocProvider<T> implements Provider<T> {

    private final T instance;
    private final Annotation qualifier;

    public AdHocProvider(T instance) {
        this(null, instance);
    }

    public AdHocProvider(Annotation qualifier, T instance) {
        this.instance = instance;
        this.qualifier = qualifier;
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
