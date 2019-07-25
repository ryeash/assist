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
     * The aspects to weave into the object instance. Only one {@link vest.assist.aop.InvokeMethod} aspect may be
     * declared; if multiple are listed an {@link IllegalArgumentException} will be thrown during processing.
     */
    Class<? extends Aspect>[] value();
}
