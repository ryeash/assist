package vest.assist.aop;

import java.lang.reflect.AnnotatedElement;

public interface MethodArgument {

    Object get();

    void set(Object value);

    AnnotatedElement annotatedElement();
}
