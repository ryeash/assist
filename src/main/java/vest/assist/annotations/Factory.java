package vest.assist.annotations;

import java.lang.annotation.*;

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
}
