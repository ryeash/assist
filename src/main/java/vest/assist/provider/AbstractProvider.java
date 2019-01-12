package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.aop.Aspect;
import vest.assist.aop.AspectList;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

/**
 * Brings together the different stages of object injection: Instantiating, Injecting, and Aspect weaving.
 */
public abstract class AbstractProvider<T> implements Provider<T> {

    protected final Assist assist;
    protected final Class<T> type;
    protected final Annotation qualifier;

    public AbstractProvider(Assist assist, Class<T> type, Annotation qualifier) {
        this.assist = assist;
        this.type = type;
        this.qualifier = qualifier;
    }

    public Annotation qualifier() {
        return qualifier;
    }

    protected abstract T create();

    protected abstract Class<? extends Aspect>[] aspects();

    @Override
    public T get() {
        // create the instance
        T instance = create();

        // inject it
        inject(instance);

        // weave in the aspects
        instance = weaveAspects(instance);

        // finished
        return instance;
    }

    protected void inject(Object instance) {
        assist.inject(instance);
    }

    protected T weaveAspects(T instance) {
        Class<? extends Aspect>[] aspects = aspects();
        if (aspects == null || aspects.length == 0) {
            return instance;
        }

        if (!type.isInterface()) {
            throw new IllegalArgumentException("aspects may not be applied to non-interfaces; [" + type + "] may not be assigned aspects");
        }

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
