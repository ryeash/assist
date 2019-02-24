package vest.assist.aop;

/**
 * An aspect that will execute after method calls.
 */
public interface AfterMethod extends Aspect {

    /**
     * Execute post method invocation actions.
     *
     * @param invocation the invocation
     * @throws Throwable for any issue encountered
     */
    void after(Invocation invocation) throws Throwable;
}
