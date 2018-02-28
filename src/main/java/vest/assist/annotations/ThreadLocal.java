package vest.assist.annotations;

import javax.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Set the scope of the provider to ThreadLocal. A thread local provider creates one instance of the provided class
 * per thread.
 */
@Scope
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ThreadLocal {
}
