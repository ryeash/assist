package vest.assist.aop;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an individual invocation of a method for the purposes of Aspect weaving.
 */
public class Invocation {
    private Object instance;
    private Method method;
    private Object[] args;

    protected Invocation(Object instance, Method method, Object[] args) {
        this.instance = instance;
        this.method = Objects.requireNonNull(method);
        this.args = args;
    }

    /**
     * Get the object the underlying method is invoked from
     */
    public Object getInstance() {
        return instance;
    }

    /**
     * Set/replace the object that the underlying method will be invoked from
     */
    public void setInstance(Object instance) {
        this.instance = instance;
    }

    /**
     * Get the method that was invoked
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Swap out the invoked method with the given method
     */
    public void setMethod(Method method) {
        this.method = Objects.requireNonNull(method);
    }

    /**
     * Get the arguments that the underlying method was called with. The returned array is not a copy, altering the
     * array will alter the eventual invocation of the underlying method.
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * Set the arguments that will be used to invoke the underlying method
     */
    public void setArgs(Object... args) {
        int length = args != null ? args.length : 0;
        if (length != method.getParameterCount()) {
            throw new IllegalArgumentException("wrong number of parameters; expected" + method.getParameterCount() + " given " + length);
        }
        this.args = args;
    }

    /**
     * Get the number of arguments passed into the method when it was invoked
     */
    public int getArgCount() {
        return args != null ? args.length : 0;
    }

    /**
     * Invoke the underlying method using the instance and arguments that are currently set on this Invocation.
     *
     * @return The result of invoking the method
     * @throws Throwable for any error caused by invoking the method
     */
    public Object invoke() throws Throwable {
        return method.invoke(instance, args);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (instance != null) {
            sb.append(instance.getClass().getSimpleName());
        } else {
            sb.append(method.getDeclaringClass().getSimpleName());
        }
        sb.append('.').append(method.getName());
        if (args == null || args.length == 0) {
            sb.append("()");
        } else {
            sb.append(Stream.of(args).map(String::valueOf).collect(Collectors.joining(", ", "(", ")")));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Invocation that = (Invocation) o;
        return Objects.equals(instance, that.instance)
                && Objects.equals(method, that.method)
                && Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instance, method) * 31 * Arrays.deepHashCode(args);
    }
}
