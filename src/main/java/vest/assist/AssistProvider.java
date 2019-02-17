package vest.assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Extension of the base {@link Provider} to add additional metadata.
 */
public interface AssistProvider<T> extends Provider<T> {

    Class<T> type();

    Annotation qualifier();

    Annotation scope();

    List<Annotation> annotations();

}
