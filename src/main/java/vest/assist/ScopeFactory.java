package vest.assist;

import java.lang.annotation.Annotation;

/**
 * Defines the contract for a class that can wrap a provider with a given scope.
 */
public interface ScopeFactory<S extends Annotation> {

    /**
     * The target scope that this factory works with.
     */
    Class<S> target();

    /**
     * Scope the given Provider, returning a scoped provider.
     *
     * @param provider The Provider to scope
     * @param scope    The scope annotation of the given Provider
     * @return A Provider that adheres to the definition of the scope returned by {@link #target()}
     */
    <T> AssistProvider<T> scope(AssistProvider<T> provider, Annotation scope);
}