package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.annotations.Eager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Eager
public class ScannedComponent {

    Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public void init() {
        log.info("scanned init");
    }
}
