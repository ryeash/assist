package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.Reflector;
import vest.assist.annotations.Aspects;
import vest.assist.annotations.Factory;
import vest.assist.aop.Aspect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * A provider instance that creates objects using a method (e.g. a @Factory method from a configuration object)
 */
public class FactoryMethodProvider<T> extends AbstractProvider<T> {

    private final Method method;
    private final Parameter[] methodParameters;
    private final Object instance;

    private final Factory factory;
    private final Class<? extends Aspect>[] aspects;

    @SuppressWarnings("unchecked")
    public FactoryMethodProvider(Method method, Object instance, Assist assist) {
        super(assist, (Class<T>) method.getReturnType(), Reflector.getQualifier(method));
        Reflector.makeAccessible(method);
        this.method = method;
        this.methodParameters = method.getParameters();
        this.instance = instance;
        this.factory = Objects.requireNonNull(method.getAnnotation(Factory.class));

        Aspects aop = method.getAnnotation(Aspects.class);
        this.aspects = aop != null ? aop.value() : null;
    }

    public boolean isEager() {
        return factory.eager();
    }

    public boolean isPrimary() {
        return factory.primary();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T create() {
        try {
            T t = (T) method.invoke(instance, assist.getParameterValues(methodParameters));
            if (t == null) {
                throw new NullPointerException("method provider [" + Reflector.detailString(method) + "] produced a null object");
            }
            return t;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("error invoking method: " + Reflector.detailString(method), e);
        }
    }

    @Override
    protected T inject(T instance) {
        if (factory.skipInjection()) {
            return instance;
        }
        return super.inject(instance);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FactoryMethodProvider{");
        if (qualifier != null) {
            sb.append(qualifier).append(":");
        }
        sb.append(method).append("}:").append(hashCode());
        return sb.toString();
    }
}

