package vest.assist.annotations;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A configuration class can use this @Scan annotation to trigger a package scan that will create/inject all classes that
 * have the target annotation type and package prefix. This can be used, for instance, to automatically wire all @Singleton annotated
 * classes in the com.foo.services package.<br><br>
 * <strong>Note:</strong> package scanning operates on classes, not providers, so configuration methods annotated with the target
 * annotation are not considered during scanning.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Scan.Scans.class)
@Documented
public @interface Scan {

    /**
     * The base packages to scan
     */
    String value();

    /**
     * The target annotation to look for on scanned classes. Classes that have the target annotation will be
     * created/injected by the Assist instance processing the configuration class.
     */
    Class<? extends Annotation> target() default Singleton.class;

    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Scans {
        Scan[] value() default {};
    }
}
