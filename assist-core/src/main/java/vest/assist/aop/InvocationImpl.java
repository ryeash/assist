package vest.assist.aop;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InvocationImpl implements Invocation {
    private final Object instance;
    private final Method method;
    private final List<MethodArgument> arguments;
    private final AspectChain chain;

    public InvocationImpl(Object instance, Method method, List<MethodArgument> arguments, AspectChain chain) {
        this.instance = instance;
        this.method = Objects.requireNonNull(method);
        this.arguments = arguments;
        this.chain = chain;
    }

    @Override
    public Object instance() {
        return instance;
    }

    /**
     * Get the method that was invoked
     */
    @Override
    public Method method() {
        return method;
    }

    @Override
    public List<MethodArgument> args() {
        return arguments;
    }

    @Override
    public MethodArgument arg(int index) {
        return arguments.get(index);
    }

    @Override
    public int arity() {
        return arguments.size();
    }

    @Override
    public Object next() throws Exception {
        return chain.next(this);
    }

    @Override
    public Object invoke() throws Exception {
        Object[] args = arguments.stream().map(MethodArgument::get).toArray();
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
        sb.append(arguments.stream().map(MethodArgument::get)
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "(", ")")));
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
        InvocationImpl that = (InvocationImpl) o;
        return Objects.equals(instance, that.instance)
                && Objects.equals(method, that.method)
                && Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instance, method, arguments);
    }
}
