package vest.assist.provider;

import vest.assist.Assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Brings together the instantiating and injecting steps of a provider.
 */
public abstract class AbstractProvider<T> implements Provider<T> {

    protected final Assist assist;
    protected final Class<T> type;
    protected final Annotation qualifier;

    public AbstractProvider(Assist assist, Class<T> type, Annotation qualifier) {
        this.assist = assist;
        this.type = type;
        this.qualifier = qualifier;
    }

    public Annotation qualifier() {
        return qualifier;
    }

    protected abstract T create();

    @Override
    public T get() {
        return inject(create());
    }

    protected T inject(T instance) {
        return assist.inject(instance);
    }
}
