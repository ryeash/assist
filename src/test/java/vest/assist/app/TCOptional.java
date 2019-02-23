package vest.assist.app;

import org.testng.Assert;

import javax.inject.Inject;
import javax.swing.*;
import java.util.Optional;

public class TCOptional {

    @Inject
    public Optional<CoffeeMaker> coffeMakerOpt;

    @Inject
    public void injectOptional(Optional<CoffeeMaker> optionalCoffee, Optional<JFrame> optionalFrame) {
        Assert.assertTrue(optionalCoffee.isPresent());
        Assert.assertTrue(optionalCoffee.get() instanceof CoffeeMaker);
        Assert.assertFalse(optionalFrame.isPresent());
    }
}
