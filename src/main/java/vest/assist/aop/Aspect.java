package vest.assist.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The basic class for weaving aspects into Assist managed applications. At a high level an Aspect breaks up invocation
 * of a method into three steps: pre (before), exec (around), post (after). Leaving all methods default (not overriding
 * any of them) will cause method execution to behave normally, i.e. no extra code executions or modification to the method
 * call.
 * <p/>
 * Implementations SHOULD be threadsafe.
 */
public abstract class Aspect implements InvocationHandler {

    protected Object instance;

    /**
     * Initialize this aspect for execution with the given object instance.
     * Subclasses needing to override this method MUST call super.init().
     */
    public void init(Object instance) {
        this.instance = instance;
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Invocation invocation = new Invocation(instance, method, args);
        // execute the pre and exec steps in a try block, this allows us to catch any exception
        // and possibly log and replace the error result in the post step
        try {
            pre(invocation);
            exec(invocation);
        } catch (Throwable t) {
            invocation.setResult(t);
        }
        post(invocation);
        if (invocation.isError()) {
            Throwable t = (Throwable) invocation.getResult();
            if (t instanceof InvocationTargetException && t.getCause() != null) {
                throw t.getCause();
            } else {
                throw t;
            }
        }
        return invocation.getResult();
    }

    /**
     * Called before execution of a method. Allows implementation to, for instance, log entry, or (alarmingly) modify
     * arguments. This method defaults to a no-op.
     *
     * @param invocation The Invocation object that tracks the references associated with the method invocation and results of the invocation
     * @throws Throwable for any unexpected exception
     */
    public void pre(Invocation invocation) throws Throwable {
        // no-op
    }

    /**
     * Called to invoke a method. Allows implementations to, for instance, time execution of the method, or (alarmingly)
     * call a completely different method on a completely different object. This method defaults to invoking the method
     * given, on the object given, using {@link Method#invoke(Object, Object...)}.
     *
     * @param invocation The Invocation object that tracks the references associated with the method invocation and results of the invocation
     * @throws Throwable for any unexpected exception
     */
    public void exec(Invocation invocation) throws Throwable {
        invocation.invoke();
    }

    /**
     * Called after execution of a method. Allows implementations to, for instance, log the result, or (alarmingly) replace
     * the result of method invocation. This method defaults to a no-op.
     *
     * @param invocation The Invocation object that tracks the references associated with the method invocation and results of the invocation
     * @throws Throwable for any unexpected exception
     */
    public void post(Invocation invocation) throws Throwable {
        // no-op
    }

}
