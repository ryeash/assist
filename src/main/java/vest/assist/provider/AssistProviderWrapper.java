package vest.assist.provider;

import vest.assist.AssistProvider;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

public abstract class AssistProviderWrapper<T> implements AssistProvider<T> {

    private final AssistProvider<T> delegate;

    protected AssistProviderWrapper(AssistProvider<T> delegate) {
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
    public T get() {
        return delegate.get();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
