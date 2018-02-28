package vest.assist.app;

import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

@Named("k")
public class Keurig implements CoffeeMaker {

    @Inject
    @Log
    private Logger log;

    @Override
    public String brew() {
        log.info("brewing");
        return "keurig";
    }
}