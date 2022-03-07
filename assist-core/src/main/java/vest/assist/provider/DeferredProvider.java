package vest.assist.provider;

import jakarta.inject.Provider;
import vest.assist.Assist;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Used internally to support {@link vest.assist.annotations.Lazy}.
 */
public class DeferredProvider<T> implements Provider<T> {

    private final Assist assist;
    private final Class<T> type;
    private final Annotation qualifier;
    private final AtomicReference<T> atom = new AtomicReference<>(null);

    public DeferredProvider(Assist assist, Class<T> type, Annotation qualifier) {
        this.assist = assist;
        this.type = type;
        this.qualifier = qualifier;
    }

    @Override
    public T get() {
        return atom.updateAndGet(v -> v != null ? v : init());
    }

    private T init() {
        return assist.instance(type, qualifier);
    }
}
