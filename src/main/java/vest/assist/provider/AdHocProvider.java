package vest.assist.provider;

import javax.inject.Provider;

/**
 * A provider that just returns an instance of an object
 */
public class AdHocProvider<T> implements Provider<T> {

    private final T instance;

    public AdHocProvider(T instance) {
        this.instance = instance;
    }

    @Override
    public T get() {
        return instance;
    }

    @Override
    public String toString() {
        return "AdHocProvider{" + instance.getClass().getSimpleName() + "}:" + hashCode();
    }

}
