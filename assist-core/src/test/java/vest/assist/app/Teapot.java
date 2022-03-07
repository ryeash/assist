package vest.assist.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.testng.Assert;

@Singleton
public class Teapot {

    // private constructor to validate that it can still be injected
    @Inject
    private Teapot(CoffeeMaker cm) {
        Assert.assertNotNull(cm);
    }
}