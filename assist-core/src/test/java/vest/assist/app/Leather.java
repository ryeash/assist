package vest.assist.app;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface Leather {

    Color color() default Color.TAN;

    enum Color {RED, BLACK, TAN}

}
