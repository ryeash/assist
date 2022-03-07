package vest.assist.provider;

import jakarta.inject.Singleton;
import vest.assist.AssistProvider;
import vest.assist.ScopeFactory;

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
    public <T> AssistProvider<T> scope(AssistProvider<T> provider, Annotation scope) {
        return new SingletonProvider<>(provider);
    }

    public static final class SingletonProvider<T> extends AssistProviderWrapper<T> {

        private volatile boolean initialized = false;
        private T value;

        public SingletonProvider(AssistProvider<T> provider) {
            super(provider);
        }

        @Override
        public T get() {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        value = super.get();
                        initialized = true;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return "@Singleton{" + super.toString() + "}";
        }
    }
}
