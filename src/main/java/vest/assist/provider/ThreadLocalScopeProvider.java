package vest.assist.provider;

import vest.assist.ScopeProvider;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * A scope that provides thread local instances of objects.
 */
public class ThreadLocalScopeProvider<T> implements ScopeProvider<T> {

    private final ThreadLocal<T> threadLocal = new ThreadLocal<>();

    @Override
    public T scope(Provider<T> provider, Annotation scope) {
        if (threadLocal.get() == null) {
            threadLocal.set(provider.get());
        }
        return threadLocal.get();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
