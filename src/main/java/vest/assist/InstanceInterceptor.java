package vest.assist;

/**
 * Receives newly instantiated objects created in {@link javax.inject.Provider}s. Allows for post instantiation field
 * and method injection, for example.
 *
 * @see vest.assist.provider.InjectAnnotationInterceptor
 */
@FunctionalInterface
public interface InstanceInterceptor extends Prioritized {

    /**
     * Intercept the given object instance before it is returned from a Provider.
     *
     * @param instance The intercepted instance
     */
    void intercept(Object instance);
}
