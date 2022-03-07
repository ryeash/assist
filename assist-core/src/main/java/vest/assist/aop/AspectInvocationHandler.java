package vest.assist.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Used internally to support aspect oriented behaviors of Assist.
 */
public class AspectInvocationHandler implements InvocationHandler {

    protected final Object instance;
    protected final List<Aspect> aspects;

    public AspectInvocationHandler(Object instance, Aspect... aspects) {
        this.instance = instance;
        this.aspects = List.of(aspects);
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<MethodArgument> arguments;
        if (args == null || args.length == 0) {
            arguments = List.of();
        } else {
            arguments = new ArrayList<>(args.length);
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Object v = args[i];
                arguments.add(new MethodArgumentImpl(v, parameter));
            }
        }
        AspectChain chain = new AspectChainImpl(aspects.iterator());
        InvocationImpl invocation = new InvocationImpl(instance, method, arguments, chain);
        try {
            return chain.next(invocation);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException && t.getCause() != null) {
                throw t.getCause();
            } else {
                throw t;
            }
        }
    }

    private record AspectChainImpl(Iterator<Aspect> iterator) implements AspectChain {

        @Override
        public Object next(InvocationImpl invocation) throws Exception {
            if (iterator.hasNext()) {
                return iterator.next().invoke(invocation);
            } else {
                return invocation.invoke();
            }
        }
    }
}
