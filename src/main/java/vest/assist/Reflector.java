package vest.assist;

import javax.inject.Qualifier;
import javax.inject.Scope;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Manages basic reflection tasks of dependency injection. Caches instances to speed up usage.
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
            Collections.addAll(typeFields, temp.getDeclaredFields());

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
    public List<Field> fields() {
        return fields;
    }

    /**
     * @return A stream of all methods defined for the reflected type; includes all access levels, static, inherited, and lambda
     * generated methods
     */
    public List<Method> methods() {
        return methods;
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

    /**
     * Attempts to make the given AccessibleObject truly accessible. Necessary for non-public fields, methods, etc. that
     * are being injected.
     *
     * @param ao The object to set accessibility for
     */
    public static void makeAccessible(AccessibleObject ao) {
        if (!ao.isAccessible()) {
            ao.setAccessible(true);
        }
    }

    /**
     * Build a detail string, suitable for debugging purposes, from the given annotated element. Useful in exception
     * cases to fully describe what reflected entity an error occurred on.
     *
     * @param annotatedElement The element to build the string from
     * @return A detailed string describing the given annotated element
     */
    public static String detailString(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Field) {
            Field f = (Field) annotatedElement;
            return "Field{"
                    + "name=" + f.getName()
                    + ", type=" + f.getType().getCanonicalName()
                    + ", declaredIn=" + debugName(f.getDeclaringClass())
                    + '}';
        } else if (annotatedElement instanceof Parameter) {
            Parameter p = (Parameter) annotatedElement;
            return "Parameter{"
                    + "name=" + p.getName()
                    + ", executable=" + p.getDeclaringExecutable()
                    + ", declaredIn=" + debugName(p.getDeclaringExecutable().getDeclaringClass())
                    + '}';
        } else if (annotatedElement instanceof Method) {
            Method m = (Method) annotatedElement;
            return "Method{"
                    + "name=" + m.getName()
                    + ", paramTypes=" + Arrays.toString(m.getParameterTypes())
                    + ", declaredIn=" + debugName(m.getDeclaringClass())
                    + '}';
        } else {
            return annotatedElement.toString();
        }
    }

    /**
     * Get the debug name of a class.
     *
     * @param c The class to get the debug name for
     * @return The debug name of the class
     */
    public static String debugName(Class c) {
        if (c.isArray()) {
            return c.getTypeName();
        } else if (c.getCanonicalName() != null) {
            return c.getCanonicalName();
        } else {
            return c.getName();
        }
    }

    /**
     * Load a class name using the Thread context class loader if possible, or else use {@link Class#forName(String)}.
     *
     * @param canonicalClassName The full name of the class to load.
     * @return The loaded class
     * @throws ClassNotFoundException if the class does not exist in the class loader
     */
    public static Class<?> loadClass(String canonicalClassName) throws ClassNotFoundException {
        if (Thread.currentThread().getContextClassLoader() != null) {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(canonicalClassName);
            } catch (ClassNotFoundException c) {
                // ignored
            }
        }
        return Class.forName(canonicalClassName);
    }

    private static final class UniqueMethod {
        private final Method method;
        private final Class<?>[] parameterTypes;

        UniqueMethod(Method method) {
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
