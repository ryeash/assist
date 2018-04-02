package vest.assist.app;


import org.slf4j.Logger;

import javax.inject.Inject;

public class FrenchPress implements CoffeeMaker, AutoCloseable {

    @Inject
    @Log
    private Logger log;

    @Override
    public String brew() {
        log.info("brewing");
        return "french";
    }

    @Override
    public void close() {
        log.info("closing the french press {}", hashCode());
    }
}
