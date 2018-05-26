package vest.assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serves as the default {@link ValueLookup} for Assist. Finds injectable
 * values based on the type. The priority for this class is 10000, and should always
 * be prioritized last in the list of registered ValueLookups for an Assist instance.
 */
public final class ProviderTypeValueLookup implements ValueLookup {

    private final Assist assist;

    public ProviderTypeValueLookup(Assist assist) {
        this.assist = assist;
    }

    @Override
    public Object lookup(Class<?> rawType, Type genericType, AnnotatedElement annotatedElement) {
        Annotation qualifier = Reflector.getQualifier(annotatedElement);
        if (rawType == Provider.class) {
            Class<?> realType = getRealType(annotatedElement, genericType);
            return assist.providerFor(realType, qualifier);
        } else if (Collection.class.isAssignableFrom(rawType)) {
            Class<?> realType = getRealType(annotatedElement, genericType);
            return assist.providersFor(realType, qualifier)
                    .map(Provider::get)
                    .collect(Collectors.toCollection(rawType == Set.class ? HashSet::new : ArrayList::new));
        } else if (Optional.class == rawType) {
            Class<?> realType = getRealType(annotatedElement, genericType);
            return assist.providersFor(realType, qualifier).findAny();
        } else {
            return assist.instance(rawType, qualifier);
        }
    }

    private Class getRealType(AnnotatedElement annotatedElement, Type genericType) {
        Class<?> realType = Reflector.getParameterizedType(genericType);
        if (realType == null) {
            throw new IllegalArgumentException(genericType.getTypeName() + " was not defined with a specific type, injection is impossible for: " + Reflector.detailString(annotatedElement));
        }
        return realType;
    }

    @Override
    public int priority() {
        return 10000;
    }
}
