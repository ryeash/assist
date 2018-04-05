package vest.assist.app;

import org.testng.Assert;

import javax.inject.Inject;
import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// TestCase: can inject Lists, Sets, Collections, and Optionals (constructor, field, and method)
public class TCCollectionInjection {

    public List<CoffeeMaker> coffeeMakers;
    public Set<CoffeeMaker> coffeeMakerSet;

    @Inject
    public Collection<CoffeeMaker> coffeeMakerCollection;

    @Inject
    public TCCollectionInjection(List<CoffeeMaker> coffeeMakers) {
        this.coffeeMakers = coffeeMakers;
    }

    @Inject
    public void injectSet(Set<CoffeeMaker> set) {
        this.coffeeMakerSet = set;
    }

    @Inject
    public void injectOptional(Optional<CoffeeMaker> optionalCoffee, Optional<JFrame> optionalFrame) {
        Assert.assertTrue(optionalCoffee.isPresent());
        Assert.assertFalse(optionalFrame.isPresent());
    }

}
