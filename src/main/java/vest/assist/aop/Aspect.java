package vest.assist.aop;

/**
 * Base Aspect class.
 *
 * @see BeforeMethod
 * @see AfterMethod
 * @see InvokeMethod
 */
public interface Aspect {

    /**
     * Initialize with the target instance that invocations will execute against.
     *
     * @param instance the object instance that method invocations will target
     */
    default void init(Object instance) {
        // no-op
    }
}
