package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.Reflector;
import vest.assist.ValueLookup;
import vest.assist.annotations.Property;
import vest.assist.conf.ConfigurationFacade;

import javax.inject.Inject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * Responsible for injecting Fields and Parameter values annotated with the {@link Property} annotation.
 */
public class PropertyInjector implements InstanceInterceptor, ValueLookup {

    private final ConfigurationFacade conf;

    public PropertyInjector(Assist assist) {
        this.conf = assist.instance(ConfigurationFacade.class);
    }

    @Override
    public void intercept(Object instance) {
        for (Field field : Reflector.of(instance).fields()) {
            if (field.isAnnotationPresent(Property.class) && !field.isAnnotationPresent(Inject.class)) {
                try {
                    Object value = getProperty(field.getType(), field.getGenericType(), field);
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    field.set(instance, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("error setting field to property value", e);
                }
            }
        }
    }

    @Override
    public Object lookup(Class<?> rawType, Type genericType, AnnotatedElement annotatedElement) {
        return getProperty(rawType, genericType, annotatedElement);
    }

    @Override
    public int priority() {
        return 900;
    }

    private Object getProperty(Class<?> rawType, Type genericType, AnnotatedElement annotatedElement) {
        Property prop = annotatedElement.getAnnotation(Property.class);
        if (prop == null) {
            return null;
        }
        if (List.class.isAssignableFrom(rawType)) {
            return conf.getList(prop.value(), Reflector.getParameterizedType(genericType));
        } else if (Set.class.isAssignableFrom(rawType)) {
            return conf.getSet(prop.value(), Reflector.getParameterizedType(genericType));
        } else {
            return conf.get(prop.value(), rawType);
        }
    }
}
