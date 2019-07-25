package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.ValueLookup;
import vest.assist.annotations.Lazy;
import vest.assist.util.Reflector;

import javax.inject.Provider;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

public class LazyLookup implements ValueLookup {

    private final Assist assist;

    public LazyLookup(Assist assist) {
        this.assist = assist;
    }

    @Override
    public Object lookup(Class<?> rawType, Type genericType, AnnotatedElement annotatedElement) {
        if (annotatedElement.isAnnotationPresent(Lazy.class)) {
            if (rawType != Provider.class) {
                throw new IllegalArgumentException("@Lazy may only be used for Provider types; illegal use on " + Reflector.detailString(annotatedElement));
            }
            Class<?> generic = Reflector.getParameterizedType(genericType);
            return assist.lazyProviderFor(generic, Reflector.getQualifier(annotatedElement));
        }
        return null;
    }

    @Override
    public int priority() {
        return 10000;
    }
}
