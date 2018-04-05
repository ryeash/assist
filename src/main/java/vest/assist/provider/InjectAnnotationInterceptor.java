package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.Reflector;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

        for (Field field : reflector.fields()) {
            if (field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(Resource.class)) {
                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    field.set(instance, assist.valueFor(field));
                } catch (Throwable e) {
                    throw new RuntimeException("could not inject field: " + field, e);
                }
            }
        }

        for (Method method : reflector.methods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                try {
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    method.invoke(instance, assist.getParameterValues(method));
                } catch (Throwable e) {
                    throw new RuntimeException("error invoking injectable method: " + method, e);
                }
            }
        }
    }

    @Override
    public int priority() {
        return 1000;
    }
}
