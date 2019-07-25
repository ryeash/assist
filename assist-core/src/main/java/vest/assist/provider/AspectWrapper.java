package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.AssistProvider;
import vest.assist.ProviderWrapper;
import vest.assist.annotations.Aspects;

import java.lang.annotation.Annotation;

/**
 * Used internally to weave aspects together with provided instances.
 */
public class AspectWrapper implements ProviderWrapper {

    private final Assist assist;

    public AspectWrapper(Assist assist) {
        this.assist = assist;
    }

    @Override
    public <T> AssistProvider<T> wrap(AssistProvider<T> provider) {
        for (Annotation annotation : provider.annotations()) {
            if (annotation.annotationType() == Aspects.class) {
                return new AspectWeaverProvider<>(assist, ((Aspects) annotation).value(), provider);
            }
        }
        return provider;
    }

    @Override
    public int priority() {
        return 50000;
    }
}
