package vest.assist.aop;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Represents an individual invocation of a method.
 */
public interface Invocation {

    /**
     * Get the object the underlying method is invoked from
     */
    Object instance();

    /**
     * Get the method that was invoked.
     */
    Method method();

    /**
     * Get the arguments that the method was invoked with.
     */
    List<MethodArgument> args();

    /**
     * Get the method argument for the given index.
     */
    MethodArgument arg(int index);

    /**
     * Get the number of parameters the method accepts.
     */
    int arity();

    /**
     * Pass aspect invocation to the next step in the aspect chain.
     *
     * @return the result of continuing the aspect execution
     */
    Object next() throws Exception;

    /**
     * Invoke the method on the instance and return the result. Unless you have a
     * need to short circuit aspect execution, the {@link #next()} method should be preferred.
     *
     * @return the result of executing the method with the current arguments
     */
    Object invoke() throws Exception;
}
