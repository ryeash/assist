package vest.assist.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.annotations.Eager;

@Singleton
@Eager
public class ScannedComponent {

    Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public void init() {
        log.info("scanned init");
    }
}
