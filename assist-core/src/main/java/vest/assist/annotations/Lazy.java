package vest.assist.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks an injected field to be lazily injected. Lazy injection allows a provider to be injected before it can be
 * properly provided, e.g. when assist.instance(...) would throw an exception. This can be useful at times when dependency
 * or inheritance issues might cause problems. Lazy should be used carefully; over use of lazy injection may
 * be an indication of architectural problems.
 * <p>
 * The @Lazy annotation may only be used to inject Provider types, e.g.:
 * <code>
 * &#64;Lazy
 * &#64;Inject
 * Provider&lt;DAO&gt; lazyDao;
 * </code>
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Extension
public @interface Lazy {
}
