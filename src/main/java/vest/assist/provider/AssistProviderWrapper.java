package vest.assist.provider;

import vest.assist.AssistProvider;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

public abstract class AssistProviderWrapper<T> implements AssistProvider<T> {

    private final AssistProvider<T> delegate;

    protected AssistProviderWrapper(AssistProvider<T> delegate) {
        if (this == delegate) {
            throw new IllegalArgumentException("refusing to wrap myself");
        }
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Class<T> type() {
        return delegate.type();
    }

    @Override
    public Annotation qualifier() {
        return delegate.qualifier();
    }

    @Override
    public Annotation scope() {
        return delegate.scope();
    }

    @Override
    public List<Annotation> annotations() {
        return delegate.annotations();
    }

    @Override
    public boolean eager() {
        return delegate.eager();
    }

    @Override
    public boolean primary() {
        return delegate.primary();
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
