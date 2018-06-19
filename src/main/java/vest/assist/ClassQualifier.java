package vest.assist;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Used internally to track class type and qualifier combinations for providers.
 */
public final class ClassQualifier {
    final Class type;
    final Annotation qualifier;

    public ClassQualifier(Class type, Annotation qualifier) {
        this.type = type;
        this.qualifier = qualifier;
    }

    public Class type() {
        return type;
    }

    public Annotation qualifier() {
        return qualifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClassQualifier that = (ClassQualifier) o;
        return Objects.equals(type, that.type) && Objects.equals(qualifier, that.qualifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, qualifier);
    }
}
