package vest.assist.app;

import java.lang.annotation.*;

@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
}
