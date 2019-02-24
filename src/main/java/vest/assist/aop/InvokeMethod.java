package vest.assist.aop;

/**
 * An aspect that executes the invoked method.
 */
public interface InvokeMethod extends Aspect {

    /**
     * Execute the invoked method performing any additional actions as needed.
     * Implementations should, at least, call {@link Invocation#invoke()}.
     *
     * @param invocation the invocation
     * @throws Throwable for any issue encountered
     */
    void invoke(Invocation invocation) throws Throwable;
}
