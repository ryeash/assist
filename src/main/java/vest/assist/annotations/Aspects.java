package vest.assist.annotations;

import vest.assist.aop.Aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a configuration method with the aspects that will be woven together with the returned value of the method.
 */
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Aspects {

    /**
     * The aspects to weave into the object instance. In cases where multiple aspects are used, all of them will have
     * their pre and post methods executed, but only the exec method of the last aspect listed will be used.
     */
    Class<? extends Aspect>[] value();
}
