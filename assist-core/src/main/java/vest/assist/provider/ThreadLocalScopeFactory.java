package vest.assist.provider;

import vest.assist.AssistProvider;
import vest.assist.ScopeFactory;
import vest.assist.annotations.ThreadLocal;

import java.lang.annotation.Annotation;

/**
 * A scope that provides thread local instances of objects.
 */
public class ThreadLocalScopeFactory implements ScopeFactory<ThreadLocal> {

    @Override
    public Class<ThreadLocal> target() {
        return ThreadLocal.class;
    }

    @Override
    public <T> AssistProvider<T> scope(AssistProvider<T> provider, Annotation scope) {
        return new ThreadLocalProvider<>(provider);
    }

    public static final class ThreadLocalProvider<T> extends AssistProviderWrapper<T> {

        private final java.lang.ThreadLocal<T> threadLocal = new java.lang.ThreadLocal<>();

        public ThreadLocalProvider(AssistProvider<T> provider) {
            super(provider);
        }

        @Override
        public T get() {
            if (threadLocal.get() == null) {
                threadLocal.set(super.get());
            }
            return threadLocal.get();
        }

        @Override
        public String toString() {
            return "@ThreadLocal{" + super.toString() + "}";
        }
    }
}
