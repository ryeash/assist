package vest.assist.app;

import jakarta.inject.Inject;

public class Child extends Parent {

    @Inject
    public void decrement() {
        log.info("child dec");
        i = -1;
        log.info("{}", i);
    }

    @Inject
    @Override
    public void toOverride() {
        log.info("this is the child");
        this.over = "child";
    }


    @Override
    public void overrideNoInject() {
    }
}
