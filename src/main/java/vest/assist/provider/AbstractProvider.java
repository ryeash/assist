package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.ScopeProvider;
import vest.assist.aop.Aspect;
import vest.assist.aop.AspectList;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

/**
 * Brings together all the different stages of object injection: Instantiating, Injecting, Scoping, and Aspect weaving.
 */
public abstract class AbstractProvider<T> implements Provider<T> {

    protected final Assist assist;
    protected final Class<T> type;
    protected final Annotation qualifier;
    protected final Annotation scope;
    private final ScopeProvider<T> scopeProvider;

    public AbstractProvider(Assist assist, Class<T> type, Annotation qualifier, Annotation scope) {
        this.assist = assist;
        this.type = type;
        this.qualifier = qualifier;
        this.scope = scope;
        if (scope != null) {
            this.scopeProvider = assist.createScopeProvider(scope);
        } else {
            this.scopeProvider = null;
        }
    }

    public Annotation qualifier() {
        return qualifier;
    }

    public Annotation scope() {
        return scope;
    }

    protected abstract T create();

    protected abstract Class<? extends Aspect>[] aspects();

    @Override
    public T get() {
        if (scopeProvider == null) {
            return newInstance();
        } else {
            return scopeProvider.scope(this::newInstance, scope());
        }
    }

    protected T newInstance() {
        // create the instance
        T instance = create();

        // inject it
        assist.inject(instance);

        // weave in the aspects
        instance = weaveAspects(instance);

        // finished
        return instance;
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
