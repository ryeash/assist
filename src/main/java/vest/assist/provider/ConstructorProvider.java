package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.Reflector;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A provider instance that creates objects using a constructor. Per spec, only the zero-arg constructor or a
 * constructor with the @Inject annotation are considered for auto creation. If neither kind of constructor is found,
 * creation of this class will fail with a RuntimeException.
 */
public class ConstructorProvider<T> extends AbstractProvider<T> {

    private final Constructor<T> constructor;
    private final Parameter[] constructorParameters;
    private final List<Annotation> annotations;
    private final Annotation scope;

    public ConstructorProvider(Class<T> type, Assist assist) {
        this(type, type, assist);
    }

    public ConstructorProvider(Class<T> advertisedType, Class<? extends T> realType, Assist assist) {
        super(assist, advertisedType, Reflector.getQualifier(realType));
        this.constructor = injectableConstructor(realType);
        this.constructorParameters = this.constructor.getParameters();
        this.annotations = Collections.unmodifiableList(Arrays.asList(realType.getAnnotations()));
        this.scope = Reflector.getScope(realType);
    }

    @Override
    public Annotation qualifier() {
        return super.qualifier();
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
    protected T create() {
        try {
            return constructor.newInstance(assist.getParameterValues(constructorParameters));
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("failed invoking constructor: " + toString(), e);
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

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> injectableConstructor(Class type) {
        if (Modifier.isAbstract(type.getModifiers()) || type.isInterface()) {
            throw new IllegalArgumentException("interfaces/abstract classes do not have injectable constructors");
        }

        // find the injectable constructors (no-arg or marked with @Inject)
        int injectAnnotatedConstructors = 0;
        LinkedList<Constructor<T>> list = new LinkedList<>();
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
            throw new RuntimeException("no injectable constructors found for " + type);
        }

        // validate that there is at most ONE @Inject constructor
        if (injectAnnotatedConstructors > 1) {
            throw new RuntimeException("not eligible for injection: '" + type.getCanonicalName() + "' - only one constructor may be marked with @Inject");
        }

        Constructor<T> constructor = list.get(0);

        Reflector.makeAccessible(constructor);
        return constructor;
    }
}
