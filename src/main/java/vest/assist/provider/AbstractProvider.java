package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.AssistProvider;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Brings together the instantiating and injecting steps of a provider.
 */
public abstract class AbstractProvider<T> implements AssistProvider<T> {

    protected final Assist assist;
    protected final Class<T> type;
    protected final Annotation qualifier;

    public AbstractProvider(Assist assist, Class<T> type, Annotation qualifier) {
        this.assist = assist;
        this.type = type;
        this.qualifier = qualifier;
    }

    @Override
    public Class<T> type() {
        return type;
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
