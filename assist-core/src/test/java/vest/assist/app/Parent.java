package vest.assist.app;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parent {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected int i = 0;

    @Inject
    public void increment() {
        log.info("parent inc");
        i = i + 1;
    }

    public int getI() {
        return i;
    }


    public String over;

    @Inject
    public void toOverride() {
        // Used to validate: "A method annotated with @Inject that overrides another method annotated with
        // @Inject will only be injected once per injection request per instance."
        log.info("this is the parent");
        over = "parent";
    }


    public String noInject;

    @Inject
    public void overrideNoInject() {
        // Use to validate: "A method with no @Inject annotation that overrides a method annotated with
        // @Inject will not be injected."
        this.noInject = "parent";
    }
}
