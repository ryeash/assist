package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.Reflector;
import vest.assist.ValueLookup;
import vest.assist.annotations.Property;
import vest.assist.conf.ConfigurationFacade;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

/**
 * Responsible for injecting Field and Parameter values annotated with {@link Property}.
 */
public class PropertyInjector implements InstanceInterceptor, ValueLookup {

    private final Provider<ConfigurationFacade> lazyConf;

    public PropertyInjector(Assist assist) {
        this.lazyConf = new LazyProvider<>(assist, ConfigurationFacade.class, null);
    }

    @Override
    public void intercept(Object instance) {
        for (Field field : Reflector.of(instance).fields()) {
            // we ignore the @Inject fields because they will be lookup up used the ValueLookup part of this class
            if (field.isAnnotationPresent(Property.class) && !field.isAnnotationPresent(Inject.class)) {
                Property prop = field.getAnnotation(Property.class);
                try {
                    Object value = getProperty(prop, field.getType(), field.getGenericType());
                    Reflector.makeAccessible(field);
                    if (value == null && prop.required()) {
                        throw new IllegalArgumentException("missing property: " + prop.value() + ", for " + Reflector.detailString(field));
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
        if (annotatedElement.isAnnotationPresent(Property.class)) {
            Property prop = annotatedElement.getAnnotation(Property.class);
            Object o = getProperty(prop, rawType, genericType);
            if (o == null && prop.required()) {
                throw new IllegalArgumentException("missing property: " + prop.value() + ", for " + Reflector.detailString(annotatedElement));
            }
            return o;
        } else {
            return null;
        }
    }

    @Override
    public int priority() {
        return 900;
    }

    private Object getProperty(Property prop, Class<?> rawType, Type genericType) {
        ConfigurationFacade conf = lazyConf.get();
        if (Set.class.isAssignableFrom(rawType)) {
            return conf.getSet(prop.value(), Reflector.getParameterizedType(genericType));
        } else if (Collection.class.isAssignableFrom(rawType)) {
            return conf.getList(prop.value(), Reflector.getParameterizedType(genericType));
        } else {
            return conf.get(prop.value(), rawType);
        }
    }
}
