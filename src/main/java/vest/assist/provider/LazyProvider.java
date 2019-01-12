package vest.assist.provider;

import vest.assist.Assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * Used internally to support {@link vest.assist.annotations.Lazy}.
 */
public class LazyProvider<T> implements Provider<T> {

    private final Assist assist;
    private final Class<T> type;
    private final Annotation qualifier;

    private T value;
    private volatile boolean initialized = false;

    public LazyProvider(Assist assist, Class<T> type, Annotation qualifier) {
        this.assist = assist;
        this.type = type;
        this.qualifier = qualifier;
    }

    @Override
    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = assist.instance(type, qualifier);
                    initialized = true;
                }
            }
        }
        return value;
    }
}
