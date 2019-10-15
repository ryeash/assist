package vest.assist.conf;

import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.ValueLookup;
import vest.assist.annotations.Property;
import vest.assist.util.FieldTarget;
import vest.assist.util.Reflector;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

/**
 * Responsible for injecting Field and Parameter values annotated with {@link Property}.
 */
public class PropertyInjector implements InstanceInterceptor, ValueLookup {

    private final Provider<ConfigurationFacade> lazyConf;

    public PropertyInjector(Assist assist) {
        this.lazyConf = assist.lazyProviderFor(ConfigurationFacade.class, null);
    }

    @Override
    public void intercept(Object instance) {
        for (FieldTarget fieldTarget : Reflector.of(instance).fields()) {
            // we ignore the @Inject fields because they will be lookup up used the ValueLookup part of this class
            if (fieldTarget.isAnnotationPresent(Property.class) && !fieldTarget.isAnnotationPresent(Inject.class)) {
                Property prop = fieldTarget.getAnnotation(Property.class);
                try {
                    Object value = getProperty(prop, fieldTarget.getType(), fieldTarget.getGenericType());
                    if (value == null && prop.required()) {
                        throw new IllegalArgumentException("missing property: " + prop.value() + ", for " + Reflector.detailString(fieldTarget));
                    }
                    fieldTarget.set(instance, value);
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
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
