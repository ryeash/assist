package vest.assist.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks an injected field to be lazily injected. Lazy injection allows a provider to be injected before it can be
 * properly provided, i.e. assist.instance(...) would throw an exception. This can be useful at times when dependency
 * or inheritance issues might cause problems. Lazy should be used carefully; over use of lazy injection may
 * be an indication of architectural problems.
 */
@Target({FIELD})
@Retention(RUNTIME)
@Documented
public @interface Lazy {
}
