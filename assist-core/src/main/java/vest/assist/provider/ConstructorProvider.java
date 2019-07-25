package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.AssistProvider;
import vest.assist.annotations.Eager;
import vest.assist.annotations.Primary;
import vest.assist.util.Reflector;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A provider instance that creates objects using a constructor. Per spec, only the zero-arg constructor or a
 * constructor with the @Inject annotation are considered for auto creation. If neither kind of constructor is found,
 * creation of this class will fail with a RuntimeException.
 */
public class ConstructorProvider<T> implements AssistProvider<T> {

    private final Class<T> advertisedType;
    private final Assist assist;
    private final Constructor<T> constructor;
    private final MethodHandle constructorHandle;
    private final Parameter[] constructorParameters;
    private final List<Annotation> annotations;
    private final Annotation scope;
    private final Annotation qualifier;
    private final boolean eager;
    private final boolean primary;

    public ConstructorProvider(Class<T> type, Assist assist) {
        this(type, type, assist);
    }

    public ConstructorProvider(Class<T> advertisedType, Class<? extends T> realType, Assist assist) {
        this.advertisedType = advertisedType;
        this.assist = assist;
        this.constructor = injectableConstructor(realType);
        try {
            this.constructorHandle = MethodHandles.lookup().unreflectConstructor(constructor);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("error making constructor handle", e);
        }
        this.constructorParameters = this.constructor.getParameters();
        this.annotations = Collections.unmodifiableList(Arrays.asList(realType.getAnnotations()));
        this.scope = Reflector.getScope(realType);
        this.qualifier = Reflector.getQualifier(realType);
        this.eager = realType.isAnnotationPresent(Eager.class);
        this.primary = realType.isAnnotationPresent(Primary.class);
    }

    @Override
    public Class<T> type() {
        return advertisedType;
    }

    @Override
    public Annotation qualifier() {
        return qualifier;
    }

    @Override
    public Annotation scope() {
        return scope;
    }

    @Override
    public List<Annotation> annotations() {
        return annotations;
    }

    @Override
    public boolean eager() {
        return eager;
    }

    @Override
    public boolean primary() {
        return primary;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        try {
            Object o = Reflector.invoke(constructorHandle, assist.getParameterValues(constructorParameters));
            return advertisedType.cast(o);
        } catch (Throwable e) {
            throw new RuntimeException("failed invoking constructor: " + this, e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConstructorProvider{");
        if (qualifier != null) {
            sb.append(qualifier).append(":");
        }
        sb.append(constructor).append("}:").append(hashCode());
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssistProvider)) {
            return false;
        }
        AssistProvider<?> that = (AssistProvider<?>) o;
        return Objects.equals(type(), that.type()) && Objects.equals(qualifier(), that.qualifier());
    }

    @Override
    public int hashCode() {
        return advertisedType.hashCode() * 31 + (qualifier != null ? qualifier.hashCode() : 0);
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> injectableConstructor(Class type) {
        if (Modifier.isAbstract(type.getModifiers()) || type.isInterface()) {
            throw new IllegalArgumentException("interfaces/abstract classes do not have injectable constructors");
        }

        // find the injectable constructors (no-arg or marked with @Inject)
        int injectAnnotatedConstructors = 0;
        Deque<Constructor<T>> list = new LinkedList<>();
        for (Constructor<T> c : type.getDeclaredConstructors()) {
            if (c.isAnnotationPresent(Inject.class)) {
                injectAnnotatedConstructors++;
                list.addFirst(c);
            } else if (c.getParameterCount() == 0) {
                list.add(c);
            }
        }

        // error out if no constructors can be injected
        if (list.isEmpty()) {
            throw new RuntimeException("no injectable constructor found for " + type);
        }

        // validate that there is at most ONE @Inject constructor
        if (injectAnnotatedConstructors > 1) {
            throw new RuntimeException("not eligible for injection: '" + type.getCanonicalName() + "' - only one constructor may be marked with @Inject");
        }

        Constructor<T> constructor = list.getFirst();

        Reflector.makeAccessible(constructor);
        return constructor;
    }
}
