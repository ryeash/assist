package vest.assist.provider;

import jakarta.inject.Provider;
import vest.assist.Assist;
import vest.assist.ValueLookup;
import vest.assist.util.Reflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Serves as the default {@link ValueLookup} for Assist. Finds injectable
 * values based on the type. The priority for this class is {@link Integer#MAX_VALUE}, and should always
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
            return provider(genericType, annotatedElement);
        } else if (Collection.class.isAssignableFrom(rawType)) {
            return collection(rawType, genericType, annotatedElement);
        } else if (Optional.class == rawType) {
            return optional(genericType, annotatedElement);
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

    private Provider provider(Type genericType, AnnotatedElement annotatedElement) {
        Class<?> realType = getRealType(annotatedElement, genericType);
        Annotation qualifier = Reflector.getQualifier(annotatedElement);
        return assist.providerFor(realType, qualifier);
    }

    private Object collection(Class collectionType, Type genericType, AnnotatedElement annotatedElement) {
        Class<?> realType = getRealType(annotatedElement, genericType);
        Annotation qualifier = Reflector.getQualifier(annotatedElement);
        Collector<Object, ?, Collection<Object>> c;
        if (SortedSet.class.isAssignableFrom(collectionType)) {
            c = Collectors.toCollection(TreeSet::new);
        } else if (Set.class.isAssignableFrom(collectionType)) {
            c = Collectors.toCollection(HashSet::new);
        } else {
            c = Collectors.toCollection(ArrayList::new);
        }
        return assist.providersFor(realType, qualifier)
                .map(Provider::get)
                .collect(c);
    }

    private Object optional(Type genericType, AnnotatedElement annotatedElement) {
        Annotation qualifier = Reflector.getQualifier(annotatedElement);
        Class<?> realType = getRealType(annotatedElement, genericType);
        return assist.providersFor(realType, qualifier).findAny().map(Provider::get);
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }
}
