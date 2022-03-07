package vest.assist.app;

import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("k")
public class Keurig implements CoffeeMaker {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public String brew() {
        log.info("brewing");
        return "keurig";
    }
}