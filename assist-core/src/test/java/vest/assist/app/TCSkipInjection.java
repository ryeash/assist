package vest.assist.app;

import jakarta.inject.Inject;
import org.testng.Assert;

public class TCSkipInjection {

    @Inject
    public void shouldNotRun() {
        Assert.fail("this method should not have been injected");
    }
}
