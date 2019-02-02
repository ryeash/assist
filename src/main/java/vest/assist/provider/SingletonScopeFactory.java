package vest.assist.provider;

import vest.assist.ScopeFactory;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;

/**
 * A scope that enforces the @Singleton nature of a Provider. Stores the singleton instance from the Provider
 * as a private variable.
 */
public class SingletonScopeFactory implements ScopeFactory<Singleton> {

    @Override
    public Class<Singleton> target() {
        return Singleton.class;
    }

    @Override
    public <T> Provider<T> scope(Provider<T> provider, Annotation scope) {
        return new SingletonProvider<>(provider);
    }

    public static final class SingletonProvider<T> implements Provider<T> {

        private Provider<T> provider;
        private volatile boolean initialized = false;
        private T value;

        public SingletonProvider(Provider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get() {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        value = provider.get();
                        initialized = true;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return "@Singleton{" + provider + "}";
        }
    }
}
