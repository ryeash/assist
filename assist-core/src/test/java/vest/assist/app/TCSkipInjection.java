package vest.assist.app;

import org.testng.Assert;

import javax.inject.Inject;

public class TCSkipInjection {

    @Inject
    public CoffeeMaker coffeMaker;

    @Inject
    public void shouldNotRun() {
        Assert.fail("this method should not have been injected");
    }
}
