package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.AssistProvider;
import vest.assist.aop.Aspect;
import vest.assist.aop.AspectList;

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used internally to weave aspects together with provided instances.
 */
public class AspectWeaverProvider<T> extends AssistProviderWrapper<T> {

    private final Assist assist;
    private final Class<? extends Aspect>[] aspects;

    public AspectWeaverProvider(Assist assist, Class<? extends Aspect>[] aspects, AssistProvider<T> delegate) {
        super(delegate);
        if (!type().isInterface()) {
            throw new IllegalArgumentException("aspects may not be applied to non-interfaces; [" + type() + "] may not be assigned aspects");
        }
        if (aspects == null || aspects.length == 0) {
            throw new IllegalArgumentException("aspect weaver must be provided at least on aspect type");
        }
        this.assist = Objects.requireNonNull(assist);
        this.aspects = aspects;
    }

    @Override
    public T get() {
        return weaveAspects(super.get());
    }

    @Override
    public String toString() {
        return "AspectWeaverProvider" + Stream.of(aspects).map(Class::getSimpleName).collect(Collectors.joining(", ", "[", "]")) + "/" + super.toString();
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
        T t = (T) Proxy.newProxyInstance(type().getClassLoader(), instance.getClass().getInterfaces(), aspect);
        return t;
    }
}
