package vest.assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Extension of the base {@link Provider} to add additional metadata.
 */
public interface AssistProvider<T> extends Provider<T> {

    /**
     * The type provided.
     */
    Class<T> type();

    /**
     * The provider qualifier, or null if there isn't one.
     */
    Annotation qualifier();

    /**
     * The scope of the provider, or null is there isn't one.
     */
    Annotation scope();

    /**
     * All annotations associated with the provided type.
     */
    List<Annotation> annotations();

}
