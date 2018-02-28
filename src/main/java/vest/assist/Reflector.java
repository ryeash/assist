package vest.assist;

import javax.inject.Qualifier;
import javax.inject.Scope;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages basic reflection tasks of dependency injection. Uses an internal cache object to speed up usage.
 */
public class Reflector {

    private static final Map<Class, Reflector> CACHE = new HashMap<>();

    /**
     * Get or create the Reflector for the given instance type. Calls to this method are exactly the same as calling
     * Reflector.of(instance.getClass()).
     *
     * @param instance The instance to analyze
     * @return The Reflector for the instance type, if the Reflector has already been created once, a cached Reflector
     * will be returned
     */
    public static Reflector of(Object instance) {
        return of(instance.getClass());
    }

    /**
     * Get or create the Reflector for the given type.
     *
     * @param type The type to analyze
     * @return The Reflector for the type, if the Reflector has already been create once, a cached Reflector will be returned
     */
    public static Reflector of(Class type) {
        if (CACHE.containsKey(type)) {
            return CACHE.get(type);
        }
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(type, Reflector::new);
        }
    }

    /**
     * Clear all cached Reflector instances.
     */
    public static void clear() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private final Class type;
    private final Annotation scope;
    private final Annotation qualifier;
    private final List<Class> hierarchy;
    private final List<Field> fields;
    private final List<Method> methods;

    private Reflector(Class type) {
        this.type = Objects.requireNonNull(type);
        this.scope = getScope(type);
        this.qualifier = getQualifier(type);

        List<Class> typeHierarchy = new LinkedList<>();
        List<Field> typeFields = new LinkedList<>();
        List<Method> typeMethods = new LinkedList<>();

        Set<Class> interfaces = new HashSet<>();
        Set<UniqueMethod> methodTracker = new HashSet<>();
        Class temp = type;
        while (temp != null && temp != Object.class) {
            typeHierarchy.add(0, temp);
            Collections.addAll(interfaces, temp.getInterfaces());

            typeFields.addAll(Arrays.asList(temp.getDeclaredFields()));

            for (Method method : temp.getDeclaredMethods()) {
                if (methodTracker.add(new UniqueMethod(method))) {
                    typeMethods.add(0, method);
                }
            }
            temp = temp.getSuperclass();
        }
        typeHierarchy.addAll(interfaces);
        this.hierarchy = Collections.unmodifiableList(typeHierarchy);
        this.methods = Collections.unmodifiableList(typeMethods);
        this.fields = Collections.unmodifiableList(typeFields);
    }

    /**
     * @return The type of this Reflector
     */
    public Class type() {
        return type;
    }

    /**
     * @return The scope of reflected type
     */
    public Annotation scope() {
        return scope;
    }

    /**
     * @return The qualifier of the reflected type
     */
    public Annotation qualifier() {
        return qualifier;
    }

    /**
     * @return The class hierarchy of the reflected type
     */
    public List<Class> hierarchy() {
        return hierarchy;
    }

    /**
     * @return A stream of all fields defined for the reflected type; includes all access levels, inherited, and static fields
     */
    public Stream<Field> fields() {
        return fields.stream();
    }

    /**
     * Perform the given action for each field that has the given annotation.
     *
     * @param annotationType The annotation to use to select fields
     * @param action         The action to perform, will be passed the annotation instance and field
     */
    public <A extends Annotation> Reflector forAnnotatedFields(Class<A> annotationType, BiConsumer<A, Field> action) {
        fields().filter(field -> field.isAnnotationPresent(annotationType))
                .forEach(field -> {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    action.accept(field.getAnnotation(annotationType), field);
                });
        return this;
    }

    /**
     * @return A stream of all methods defined for the reflected type; includes all access levels, static, inherited, and lambda
     * generated methods
     */
    public Stream<Method> methods() {
        return methods.stream();
    }

    public <A extends Annotation> List<Method> methodsWithAnnotation(Class<A> annotationType) {
        return methods.stream().filter(m -> m.isAnnotationPresent(annotationType)).collect(Collectors.toList());
    }

    /**
     * Perform the given action for each method that has the given annotation.
     *
     * @param annotationType The annotation to use to select methods
     * @param action         The action to perform, will be passed the annotation instance and method
     */
    public <A extends Annotation> Reflector forAnnotatedMethods(Class<A> annotationType, BiConsumer<A, Method> action) {
        methods().filter(method -> method.isAnnotationPresent(annotationType))
                .forEach(method -> {
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    action.accept(method.getAnnotation(annotationType), method);
                });
        return this;
    }

    @Override
    public String toString() {
        return "Reflector[" + type.getCanonicalName() + "]";
    }

    public static Annotation getQualifier(AnnotatedElement annotatedElement) {
        return getAnnotationWithExtension(annotatedElement, Qualifier.class);
    }

    public static Annotation getScope(AnnotatedElement annotatedElement) {
        return getAnnotationWithExtension(annotatedElement, Scope.class);
    }

    public static Annotation getAnnotationWithExtension(AnnotatedElement annotatedElement, Class<? extends Annotation> parentAnnotation) {
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            if (annotation.annotationType() == parentAnnotation) {
                return annotation;
            }
            for (Annotation extendsAnnotations : annotation.annotationType().getAnnotations()) {
                if (extendsAnnotations.annotationType() == parentAnnotation) {
                    return annotation;
                }
            }
        }
        return null;
    }

    /**
     * Get the first type parameter for the given type.
     *
     * @param t The type
     * @return the first type argument if the type is a ParameterizedType and has 1 or more type arguments
     * otherwise return null
     */
    public static Class<?> getParameterizedType(Type t) {
        if (t instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) t).getActualTypeArguments();
            if (typeArgs.length > 0) {
                return (Class<?>) typeArgs[0];
            }
        }
        return null;
    }

    private static final class UniqueMethod {
        private final Method method;
        private final Class<?>[] parameterTypes;

        public UniqueMethod(Method method) {
            this.method = method;
            this.parameterTypes = method.getParameterTypes();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UniqueMethod um = (UniqueMethod) o;
            Method other = um.method;

            return Objects.equals(method.getName(), other.getName())
                    && Objects.equals(method.getReturnType(), other.getReturnType())
                    && Arrays.equals(parameterTypes, um.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method.getName(), method.getReturnType()) * Arrays.hashCode(parameterTypes);
        }
    }
}
