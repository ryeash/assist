package vest.assist.app;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrenchPress implements CoffeeMaker, AutoCloseable {

    private Logger log = LoggerFactory.getLogger(getClass());

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
