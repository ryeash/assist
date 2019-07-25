package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.AssistProvider;
import vest.assist.annotations.Eager;
import vest.assist.annotations.Primary;
import vest.assist.util.MethodTarget;
import vest.assist.util.Reflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A provider instance that creates objects using a method (e.g. a @Factory method from a configuration object)
 */
public class FactoryMethodProvider<T> implements AssistProvider<T> {

    private final Assist assist;
    private final Class<T> type;
    private final Annotation qualifier;
    private final Annotation scope;
    private final MethodTarget method;
    private final Parameter[] methodParameters;
    private final Object instance;
    private final List<Annotation> annotations;
    private final boolean eager;
    private final boolean primary;

    @SuppressWarnings("unchecked")
    public FactoryMethodProvider(MethodTarget method, Object instance, Assist assist) {
        this.type = (Class<T>) method.getReturnType();
        this.qualifier = Reflector.getQualifier(method);
        this.scope = Reflector.getScope(method);
        this.assist = assist;
        this.method = method;
        this.methodParameters = method.getParameters();
        this.instance = instance;
        this.annotations = Collections.unmodifiableList(Arrays.asList(method.getAnnotations()));
        this.eager = method.isAnnotationPresent(Eager.class);
        this.primary = method.isAnnotationPresent(Primary.class);
    }

    @Override
    public Class<T> type() {
        return type;
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
            T t = (T) method.invoke(instance, assist.getParameterValues(methodParameters));
            if (t == null) {
                throw new NullPointerException("method provider [" + Reflector.detailString(method) + "] produced a null object");
            }
            return t;
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("error invoking method: " + Reflector.detailString(method), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FactoryMethodProvider{");
        if (qualifier() != null) {
            sb.append(qualifier()).append(":");
        }
        sb.append(Reflector.detailString(method)).append("}:").append(hashCode());
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
        return type.hashCode() * 31 + (qualifier != null ? qualifier.hashCode() : 0);
    }
}

