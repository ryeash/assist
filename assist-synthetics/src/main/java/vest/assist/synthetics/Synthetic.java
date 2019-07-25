package vest.assist.synthetics;

import vest.assist.aop.Aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an application configuration class. Enables classpath scanning and automatic discovery of
 * configuration sources.
 */
@Repeatable(Synthetic.Synthetics.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Synthetic {

    Class<?> target();

    Class<? extends Aspect>[] aspects();

    String name() default "";

    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Synthetics {
        Synthetic[] value() default {};
    }
}
