package vest.assist.app;

import org.testng.Assert;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Teapot {

    // private constructor to validate that it can still be injected
    @Inject
    private Teapot(CoffeeMaker cm) {
        Assert.assertNotNull(cm);
    }
}