package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.ValueLookup;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class LogValueLookup implements ValueLookup {
    @Override
    public Object lookup(Class<?> rawType, Type genericType, AnnotatedElement annotatedElement) {
        if (annotatedElement.isAnnotationPresent(Log.class) && rawType == Logger.class) {
            return LoggerFactory.getLogger(((Parameter) annotatedElement).getDeclaringExecutable().getDeclaringClass());
        } else {
            return null;
        }
    }
}
