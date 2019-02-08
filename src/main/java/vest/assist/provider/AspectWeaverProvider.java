package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.aop.Aspect;
import vest.assist.aop.AspectList;

import javax.inject.Provider;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used internally to weave aspects together with provided instances.
 */
public class AspectWeaverProvider<T> implements Provider<T> {

    private final Assist assist;
    private final Class<? extends Aspect>[] aspects;
    private final Class<T> type;
    private final Provider<T> delegate;

    public AspectWeaverProvider(Assist assist, Class<? extends Aspect>[] aspects, Class<T> type, Provider<T> delegate) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("aspects may not be applied to non-interfaces; [" + type + "] may not be assigned aspects");
        }
        if (aspects == null || aspects.length == 0) {
            throw new IllegalArgumentException("aspect weaver must be provided at least on aspect type");
        }
        this.assist = Objects.requireNonNull(assist);
        this.aspects = aspects;
        this.type = Objects.requireNonNull(type);
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public T get() {
        return weaveAspects(delegate.get());
    }

    @Override
    public String toString() {
        return "AspectWeaverProvider" + Stream.of(aspects).map(Class::getSimpleName).collect(Collectors.joining(", ", "[", "]")) + "/" + delegate;
    }

    private T weaveAspects(T instance) {
        Aspect aspect;
        if (aspects.length == 1) {
            aspect = assist.instance(aspects[0]);
        } else {
            Aspect[] aspectsArray = new Aspect[aspects.length];
            for (int i = 0; i < aspectsArray.length; i++) {
                aspectsArray[i] = assist.instance(aspects[i]);
            }
            aspect = new AspectList(aspectsArray);
        }
        aspect.init(instance);

        @SuppressWarnings("unchecked")
        T t = (T) Proxy.newProxyInstance(type.getClassLoader(), instance.getClass().getInterfaces(), aspect);
        return t;
    }
}
