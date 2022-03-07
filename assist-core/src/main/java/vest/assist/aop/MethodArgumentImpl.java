package vest.assist.aop;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.util.Objects;

public class MethodArgumentImpl implements MethodArgument {

    private Object value;
    private final Parameter parameter;

    public MethodArgumentImpl(Object initialValue, Parameter parameter) {
        this.value = initialValue;
        this.parameter = parameter;
    }

    @Override
    public Object get() {
        return value;
    }

    @Override
    public void set(Object value) {
        this.value = value;
    }

    @Override
    public AnnotatedElement annotatedElement() {
        return parameter;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodArgumentImpl that = (MethodArgumentImpl) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
