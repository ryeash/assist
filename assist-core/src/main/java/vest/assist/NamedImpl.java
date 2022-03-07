package vest.assist;

import jakarta.inject.Named;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Used internally to turn a String into an instance of the @Named annotation
 */
public class NamedImpl implements Named {
    private final String value;

    public NamedImpl(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public String value() {
        return this.value;
    }

    public int hashCode() {
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    public boolean equals(Object o) {
        return (o instanceof Named) && value.equals(((Named) o).value());
    }

    public String toString() {
        return "@" + Named.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }
}
