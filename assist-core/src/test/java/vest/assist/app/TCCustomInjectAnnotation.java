package vest.assist.app;

import org.slf4j.Logger;

import javax.inject.Inject;

public class TCCustomInjectAnnotation {

    @Inject
    @Log
    private Logger log;

    public void logSomething() {
        log.info("something");
    }
}
