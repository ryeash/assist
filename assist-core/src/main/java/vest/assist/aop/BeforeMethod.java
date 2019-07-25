package vest.assist.aop;

/**
 * An aspect that will execute before method invocations.
 */
public interface BeforeMethod extends Aspect {

    /**
     * Execute pre method invocation actions.
     *
     * @param invocation the invocation
     * @throws Throwable for any issue encountered
     */
    void before(Invocation invocation) throws Throwable;
}
