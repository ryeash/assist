package vest.assist.aop;

/**
 * An aspect that will execute after method calls.
 */
public interface AfterMethod extends Aspect {

    /**
     * Execute post method invocation actions.
     *
     * @param invocation the invocation
     * @param result     the result of the method invocation
     * @return the result of the method invocation
     * @throws Throwable for any issue encountered
     */
    Object after(Invocation invocation, Object result) throws Throwable;
}
