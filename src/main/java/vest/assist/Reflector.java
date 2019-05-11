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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Manages basic reflection tasks of dependency injection. Caches instances to speed up usage.
 */
public class Reflector {

    private static final Map<ClassKey, Reflector> CACHE = new ConcurrentHashMap<>(64, .9F, 4);
    private static final Map<AnnotatedElement, Annotation> QUALIFIER_CACHE = new ConcurrentHashMap<>(64, .9F, 4);

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
        return CACHE.computeIfAbsent(new ClassKey(type), Reflector::new);
    }

    /**
     * Clear all cached Reflector instances.
     */
    public static void clear() {
        CACHE.clear();
        QUALIFIER_CACHE.clear();
    }

    private final Class type;
    private final String simpleName;
    private final Annotation scope;
    private final Annotation qualifier;
    private final Collection<Class> hierarchy;
    private final Collection<Field> fields;
    private final Collection<Method> methods;

    private Reflector(ClassKey key) {
        this.type = Objects.requireNonNull(key.type);
        this.simpleName = this.type.getSimpleName();
        this.scope = getScope(type);
        this.qualifier = getQualifier(type);

        LinkedList<Class> typeHierarchy = new LinkedList<>(Arrays.asList(type.getInterfaces()));
        LinkedList<Method> typeMethods = new LinkedList<>();
        Set<UniqueMethod> methodTracker = new HashSet<>(32);
        Set<Field> typeFields = new HashSet<>(16);

        Class temp = type;
        while (temp != null && temp != Object.class) {
            typeHierarchy.addFirst(temp);
            Collections.addAll(typeFields, temp.getDeclaredFields());
            for (Method method : temp.getDeclaredMethods()) {
                if (methodTracker.add(new UniqueMethod(method))) {
                    typeMethods.addFirst(method);
                }
            }
            temp = temp.getSuperclass();
        }

        this.hierarchy = Collections.unmodifiableCollection(typeHierarchy);
        this.methods = Collections.unmodifiableCollection(typeMethods);
        this.fields = Collections.unmodifiableCollection(typeFields);
    }

    /**
     * @return The type of this Reflector
     */
    public Class type() {
        return type;
    }

    /**
     * @return The simple class name of the reflected type
     */
    public String simpleName() {
        return simpleName;
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
    public Collection<Class> hierarchy() {
        return hierarchy;
    }

    /**
     * @return A stream of all fields defined for the reflected type; includes all access levels, inherited, and static fields
     */
    public Collection<Field> fields() {
        return fields;
    }

    /**
     * @return A stream of all methods defined for the reflected type; includes all access levels, static, inherited, and lambda
     * generated methods
     */
    public Collection<Method> methods() {
        return methods;
    }

    @Override
    public String toString() {
        return "Reflector[" + type + "]";
    }

    /**
     * Get the first annotation on the given element identified as a qualifier annotation. A qualifier must have the
     * {@link Qualifier} annotation attached to it. See {@link javax.inject.Named} as an example.
     *
     * @param annotatedElement The element to get the qualifier from
     * @return The first qualifier found on the element or null if none exists
     */
    public static Annotation getQualifier(AnnotatedElement annotatedElement) {
        return QUALIFIER_CACHE.computeIfAbsent(annotatedElement, ae -> getAnnotationWithExtension(ae, Qualifier.class));
    }

    /**
     * Get the first annotation on the given element identified as a scope annotation. A scope must have the
     * {@link Scope} annotation attached to it. See {@link javax.inject.Singleton} as an example.
     *
     * @param annotatedElement the element to get the scope from
     * @return The first scope found on the element or null if none exists
     */
    public static Annotation getScope(AnnotatedElement annotatedElement) {
        return getAnnotationWithExtension(annotatedElement, Scope.class);
    }

    private static Annotation getAnnotationWithExtension(AnnotatedElement annotatedElement, Class<? extends Annotation> parentAnnotation) {
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
     * @return the first type argument if the type is a {@link ParameterizedType} and has 1 or more type arguments,
     * otherwise returns null
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
     * Attempts to make the given {@link AccessibleObject} truly accessible. Necessary for non-public fields, methods, etc. that
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
            return method.getName().hashCode()
                    + 31 * method.getReturnType().toString().hashCode()
                    + 31 * Arrays.hashCode(parameterTypes);
        }
    }

    private static final class ClassKey {
        private final Class type;
        private final int hash;

        public ClassKey(Class type) {
            this.type = type;
            this.hash = type.toString().hashCode();
        }

        public Class type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            return o != null
                    && o.getClass() == getClass()
                    && type.equals(((ClassKey) o).type);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
