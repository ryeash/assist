package vest.assist;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Used internally to track class type and qualifier combinations for Providers.
 */
public final class ClassQualifier {
    private final Class type;
    private final Annotation qualifier;
    private final int hash;

    public ClassQualifier(Class type, Annotation qualifier) {
        this.type = type;
        this.qualifier = qualifier;
        this.hash = Objects.hash(type.getName(), qualifier);
    }

    public Class type() {
        return type;
    }

    public Annotation qualifier() {
        return qualifier;
    }

    @Override
    public boolean equals(Object o) {
        return o != null
                && o.getClass() == getClass()
                && type.equals(((ClassQualifier) o).type)
                && Objects.equals(qualifier, ((ClassQualifier) o).qualifier);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
