package vest.assist.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Used internally to support aspect oriented behaviors of Assist.
 */
public class AspectInvocationHandler implements InvocationHandler {

    protected final Object instance;
    private List<BeforeMethod> beforeMethods;
    private InvokeMethod invoke;
    private List<AfterMethod> afterMethods;

    public AspectInvocationHandler(Object instance, Aspect[] aspects) {
        this.instance = instance;
        beforeMethods = new LinkedList<>();
        afterMethods = new LinkedList<>();
        for (Aspect aspect : aspects) {
            if (aspect instanceof BeforeMethod) {
                beforeMethods.add((BeforeMethod) aspect);
            } else if (aspect instanceof InvokeMethod) {
                if (invoke != null) {
                    throw new IllegalArgumentException("only one InvokeMethod aspect may be assigned, found "
                            + invoke.getClass().getSimpleName() + " and " + aspect.getClass().getSimpleName());
                }
                invoke = (InvokeMethod) aspect;
            } else if (aspect instanceof AfterMethod) {
                afterMethods.add((AfterMethod) aspect);
            } else {
                throw new IllegalArgumentException("unknown aspect implementation: " + aspect.getClass().getSimpleName());
            }
            aspect.init(instance);
        }
        if (invoke == null) {
            invoke = Invocation::invoke;
        }
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Invocation invocation = new Invocation(instance, method, args);
        try {
            for (BeforeMethod beforeMethod : beforeMethods) {
                beforeMethod.before(invocation);
            }
            invoke.invoke(invocation);
        } catch (Throwable t) {
            invocation.setResult(t);
        }
        for (AfterMethod afterMethod : afterMethods) {
            afterMethod.after(invocation);
        }
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
}
