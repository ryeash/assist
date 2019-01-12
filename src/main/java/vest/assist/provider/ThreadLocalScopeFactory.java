package vest.assist.provider;

import vest.assist.ScopeFactory;
import vest.assist.annotations.ThreadLocal;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * A scope that provides thread local instances of objects.
 */
public class ThreadLocalScopeFactory implements ScopeFactory<ThreadLocal> {

    private final java.lang.ThreadLocal<Object> threadLocal = new java.lang.ThreadLocal<>();

    @Override
    public Class<ThreadLocal> target() {
        return ThreadLocal.class;
    }

    @Override
    public <T> Provider<T> scope(Provider<T> provider, Annotation scope) {
        if (threadLocal.get() == null) {
            threadLocal.set(provider.get());
        }
        return new ThreadLocalProvider<>(provider);
    }

    public static final class ThreadLocalProvider<T> implements Provider<T> {

        private final java.lang.ThreadLocal<T> threadLocal = new java.lang.ThreadLocal<>();
        private final Provider<T> provider;

        public ThreadLocalProvider(Provider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get() {
            if (threadLocal.get() == null) {
                threadLocal.set(provider.get());
            }
            return threadLocal.get();
        }

        @Override
        public String toString() {
            return "@ThreadLocal{" + provider + "}";
        }
    }
}
