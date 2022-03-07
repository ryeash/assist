package vest.assist.provider;

import jakarta.inject.Inject;
import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.util.MethodTarget;
import vest.assist.util.Reflector;

import java.util.Objects;

/**
 * An instance interceptor that follows the javax.inject specifications for setting/calling @Inject annotated fields and methods.
 */
public class InjectAnnotationInterceptor implements InstanceInterceptor {

    private final Assist assist;

    public InjectAnnotationInterceptor(Assist assist) {
        this.assist = assist;
    }

    @Override
    public void intercept(Object instance) {
        Objects.requireNonNull(instance, "null pointers can not be injected");
        Reflector reflector = Reflector.of(instance);
        for (MethodTarget method : reflector.methods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                try {
                    method.invoke(instance, assist.getParameterValues(method.getParameters()));
                } catch (Throwable e) {
                    throw new RuntimeException("error invoking injectable method: " + Reflector.detailString(method), e);
                }
            }
        }
    }

    @Override
    public int priority() {
        return 1000;
    }
}
