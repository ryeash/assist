package vest.assist.aop;

/**
 * An aspect that executes the invoked method.
 */
public interface InvokeMethod extends Aspect {

    /**
     * Execute the invoked method, performing any additional actions as needed.
     *
     * @param invocation the invocation
     * @return the result of the method invocation
     * @throws Throwable for any issue encountered
     */
    Object invoke(Invocation invocation) throws Throwable;
}
