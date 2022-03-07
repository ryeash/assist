package vest.assist.app;

import jakarta.inject.Inject;
import org.testng.Assert;

import javax.swing.*;
import java.util.Optional;

public class TCOptional {

    public Optional<CoffeeMaker> coffeeMakerOpt;

    @Inject
    public TCOptional(Optional<CoffeeMaker> coffeeMakerOpt) {
        this.coffeeMakerOpt = coffeeMakerOpt;
    }

    @Inject
    public void injectOptional(Optional<CoffeeMaker> optionalCoffee, Optional<JFrame> optionalFrame) {
        Assert.assertTrue(optionalCoffee.isPresent());
        Assert.assertTrue(optionalCoffee.get() instanceof CoffeeMaker);
        Assert.assertFalse(optionalFrame.isPresent());
    }
}
