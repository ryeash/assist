package vest.assist.app;

import jakarta.inject.Inject;
import org.slf4j.Logger;

public class TCCustomInjectAnnotation {

    private Logger log;

    @Inject
    public TCCustomInjectAnnotation(@Log Logger log) {
        this.log = log;
    }

    public void logSomething() {
        log.info("something");
    }
}
