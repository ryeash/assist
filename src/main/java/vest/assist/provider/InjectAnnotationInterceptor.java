package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.Reflector;

import javax.inject.Inject;
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
        // inject fields first
        reflector.forAnnotatedFields(Inject.class, (inject, field) -> {
            try {
                field.set(instance, assist.valueFor(field));
            } catch (Throwable e) {
                throw new RuntimeException("could not inject field: " + field, e);
            }
        });
        // inject methods second, in descending order from parent to child
        reflector.forAnnotatedMethods(Inject.class, (inject, method) -> {
            try {
                method.invoke(instance, assist.getParameterValues(method));
            } catch (Throwable e) {
                throw new RuntimeException("error invoking injectable method: " + method, e);
            }
        });
    }

    @Override
    public int priority() {
        return 1000;
    }
}
