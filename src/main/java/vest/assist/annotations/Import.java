package vest.assist.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on configuration classes to automatically include the listed classes when processing via one of the Assist
 * <code>addConfig</code> methods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

    /**
     * The classes to import when processing the target configuration class.
     */
    Class<?>[] value();
}
