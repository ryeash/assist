package vest.assist.synthetic;

import vest.assist.Reflector;
import vest.assist.annotations.Property;
import vest.assist.aop.Aspect;
import vest.assist.aop.Invocation;
import vest.assist.conf.ConfigurationFacade;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

public class SyntheticPropertiesAspect extends Aspect {

    @Inject
    private ConfigurationFacade configurationFacade;

    @Override
    public void exec(Invocation invocation) {
        Method method = invocation.getMethod();
        Property property = invocation.getMethod().getAnnotation(Property.class);
        if (method.getReturnType() == Void.TYPE) {
            invocation.setResult(null);
            return;
        }
        if (property == null) {
            throw new IllegalStateException("no property annotation on property definition interface method: " + method);
        }
        if (invocation.getArgCount() != 0) {
            throw new IllegalStateException("property definition method must not take arguments");
        }
        String name = property.value();
        Object o = getConfig(name, method.getReturnType(), method.getGenericReturnType());
        if (o == null && property.required()) {
            throw new IllegalArgumentException("no property found for: " + name + " for method " + Reflector.detailString(method));
        }
        invocation.setResult(o);
    }

    private Object getConfig(String name, Class<?> rawType, Type genericType) {
        if (Collection.class.isAssignableFrom(rawType)) {
            Class<?> gen = Reflector.getParameterizedType(genericType);
            if (Set.class.isAssignableFrom(rawType)) {
                return configurationFacade.getSet(name, gen);
            } else {
                return configurationFacade.getList(name, gen);
            }
        } else {
            return configurationFacade.get(name, rawType);
        }
    }
}
