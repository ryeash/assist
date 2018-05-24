package vest.assist.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a field or parameter to be wired with a property value pulled from the {@link vest.assist.conf.ConfigurationFacade}
 * made available via injection.
 */
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
@Documented
public @interface Property {

    /**
     * The name of the property to get the value for
     */
    String value();

    /**
     * When true and a property can not be found (the ConfigurationFacade returns null for the given property name)
     * an exception will be thrown when injecting the property target.
     */
    boolean required() default true;
}
