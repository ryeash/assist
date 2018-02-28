package vest.assist;

import vest.assist.provider.SingletonScopeProvider;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

/**
 * A ScopeProvider enforces the scoping of a Provider instance; e.g. the {@link SingletonScopeProvider}
 * enforces that only one object instance may be created by a single Provider. Implementations of this interface
 * MUST be eligible for injection (see {@link javax.inject.Inject}), else you will get a RuntimeException when it gets used.
 * Implementations of this interface should not have a scope.
 */
public interface ScopeProvider<T> {

    /**
     * Scope the given Provider, returning the scoped object instance (which may be a cached instance).
     *
     * @param provider The Provider to scope
     * @param scope    The scope annotation of the given Provider
     * @return An instance produced by the Provider
     */
    T scope(Provider<T> provider, Annotation scope);
}