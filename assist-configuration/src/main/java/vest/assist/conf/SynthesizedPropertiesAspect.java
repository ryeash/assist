package vest.assist.conf;

import vest.assist.annotations.Property;
import vest.assist.aop.Invocation;
import vest.assist.aop.InvokeMethod;
import vest.assist.util.Reflector;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class SynthesizedPropertiesAspect implements InvokeMethod {

    private final ConfigurationFacade configurationFacade;
    private final Map<Method, Object> methodResultCache;

    private Object proxy;

    @Inject
    public SynthesizedPropertiesAspect(ConfigurationFacade configurationFacade) {
        this.configurationFacade = configurationFacade;
        this.methodResultCache = new HashMap<>(32);
    }

    @Override
    public void init(Object instance) {
        this.proxy = instance;
    }

    @Override
    public Object invoke(Invocation invocation) {
        Method method = invocation.getMethod();
        // TODO
        if (methodResultCache.containsKey(method)) {
            return methodResultCache.get(method);
        }
        if (method.getReturnType() == Void.class) {
            return null;
        }
        if (method.isDefault()) {
            throw new UnsupportedOperationException("default method execution is not supported with synthetics");
        } else {
            Property annotation = method.getAnnotation(Property.class);
            if (annotation != null) {
                Object o = getProp(annotation, method.getReturnType(), Reflector.getParameterizedType(method.getGenericReturnType()));
                if (o == null && annotation.required()) {
                    throw new IllegalArgumentException("the property " + annotation.value() + " is marked as required on " + Reflector.detailString(method));
                }
                methodResultCache.put(invocation.getMethod(), o);
                return o;
            }
        }
        return null;
    }


    private Object getProp(Property annotation, Class<?> returnType, Class<?> genericType) {
        if (Collection.class.isAssignableFrom(returnType)) {
            if (Set.class.isAssignableFrom(returnType)) {
                return configurationFacade.getSet(annotation.value(), genericType);
            } else if (List.class.isAssignableFrom(returnType)) {
                return configurationFacade.getList(annotation.value(), genericType);
            } else {
                throw new UnsupportedOperationException("unhandled collection type: " + Reflector.debugName(genericType));
            }
        } else if (Stream.class.isAssignableFrom(returnType)) {
            Class<?> type = Reflector.getParameterizedType(genericType);
            return configurationFacade.getStream(annotation.value(), type);
        }
        return configurationFacade.get(annotation.value(), returnType);
    }
}
