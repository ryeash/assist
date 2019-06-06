package vest.assist.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a configuration class as a Factory that can be used to create a MethodProvider
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Factory {

    /**
     * True marks the annotated factory method as eager. Eager factories will automatically be called once during
     * configuration processing. Though it's possible to make any factory eager,
     * it really only makes sense for {@link javax.inject.Singleton}s.
     *
     * @default false
     */
    boolean eager() default false;

    /**
     * True marks the annotated factory method as primary, causing it to be registered with both it's assigned qualifier
     * (if it has one) and the null qualifier.
     *
     * @default false
     */
    boolean primary() default false;

    /**
     * True marks the annotated factory method to skip the instance interception stage of the provider workflow. Use
     * sparingly as this will prevent many of the lifecycle management tasks for provided instances.
     *
     * @default false
     */
    boolean skipInjection() default false;
}
