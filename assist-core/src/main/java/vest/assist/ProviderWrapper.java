package vest.assist;

/**
 * Defines a class that can wrap an incoming provider and return a type compatible provider. ProviderWrappers can inject
 * additional logic beyond what is possible using just {@link InstanceInterceptor}s or {@link ValueLookup}s. For example,
 * the aspect weaving support in Assist is handled by {@link vest.assist.provider.AspectWrapper}.
 */
public interface ProviderWrapper extends Prioritized {

    /**
     * Wrap the given provider and return a type compatible provider.
     *
     * @param provider the provider to wrap
     * @return a provider
     */
    <T> AssistProvider<T> wrap(AssistProvider<T> provider);
}
