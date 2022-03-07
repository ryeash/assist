package vest.assist.provider;

import jakarta.inject.Provider;
import vest.assist.Assist;
import vest.assist.ValueLookup;
import vest.assist.annotations.Property;
import vest.assist.conf.ConfigurationFacade;
import vest.assist.util.Reflector;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

/**
 * Responsible for injecting Field and Parameter values annotated with {@link Property}.
 */
public class PropertyInjector implements ValueLookup {

    private final Provider<ConfigurationFacade> lazyConf;

    public PropertyInjector(Assist assist) {
        this.lazyConf = assist.deferredProvider(ConfigurationFacade.class, null);
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
