package vest.assist;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * Defines a class that can find injectable values. During injection, the Assist will iterate through
 * the registered ValueLookups requesting a value until one returns a non-null value, once found that value is returned
 * and the iteration stops.
 */
public interface ValueLookup extends Prioritized {

    /**
     * Look up the injectable value for the given {@link AnnotatedElement}.
     *
     * @param rawType          The raw type for the injection target
     * @param genericType      The generic type for the injection target, can be null
     * @param annotatedElement The injection target
     * @return The injection value for the annotated element, returns null if this ValueLookup does not support the
     * combination of type(s) and annotations
     */
    Object lookup(Class<?> rawType, Type genericType, AnnotatedElement annotatedElement);
}
