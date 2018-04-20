package vest.assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
            throw new IllegalArgumentException(genericType.getTypeName() + " was not defined with a specific type, injection is impossible for: " + detailString(annotatedElement));
        }
        return realType;
    }

    @Override
    public int priority() {
        return 10000;
    }

    public static String detailString(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Field) {
            Field f = (Field) annotatedElement;
            return "Field{"
                    + "name=" + f.getName()
                    + ", type=" + f.getType().getCanonicalName()
                    + ", declaredIn=" + f.getDeclaringClass().getCanonicalName()
                    + '}';
        } else if (annotatedElement instanceof Parameter) {
            Parameter p = (Parameter) annotatedElement;
            if (p.getDeclaringExecutable() instanceof Method) {
                Method m = (Method) p.getDeclaringExecutable();
                return "Parameter{"
                        + "name=" + p.getName()
                        + ", method=" + m
                        + ", declaredIn=" + m.getDeclaringClass().getCanonicalName()
                        + '}';
            } else if (p.getDeclaringExecutable() instanceof Constructor) {
                Constructor c = (Constructor) p.getDeclaringExecutable();
                return "Parameter{"
                        + "name=" + p.getName()
                        + ", constructor=" + c
                        + ", declaredIn=" + c.getDeclaringClass().getCanonicalName()
                        + '}';
            } else {
                return "Parameter{"
                        + "name=" + p.getName()
                        + ", executable=" + p.getDeclaringExecutable()
                        + '}';
            }
        } else {
            return annotatedElement.toString();
        }
    }
}
