package vest.assist.provider;

import vest.assist.AssistProvider;
import vest.assist.annotations.Factory;

import java.lang.annotation.Annotation;

/**
 * Used internally to support {@link Factory#primary().
 */
public class PrimaryProvider<T> extends AssistProviderWrapper<T> {

    public PrimaryProvider(AssistProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public Annotation qualifier() {
        return null;
    }

    @Override
    public String toString() {
        return "Primary/" + super.toString();
    }
}
