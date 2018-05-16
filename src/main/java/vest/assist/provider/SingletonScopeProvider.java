package vest.assist.provider;

import vest.assist.ScopeProvider;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * A scope that enforces the @Singleton nature of a Provider. Stores the singleton instance from the Provider
 * as a private variable.
 */
public class SingletonScopeProvider<T> implements ScopeProvider<T> {
    private volatile boolean initialized;
    private T value;

    public T scope(Provider<T> provider, Annotation scope) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = provider.get();
                    initialized = true;
                    return value;
                }
            }
        }
        return value;
    }
}
