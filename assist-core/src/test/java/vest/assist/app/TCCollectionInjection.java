package vest.assist.app;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;

// TestCase: can inject Lists, Sets, Collections, and Optionals (constructor, field, and method)
public class TCCollectionInjection {

    public List<CoffeeMaker> coffeeMakers;
    public Set<CoffeeMaker> coffeeMakerSet;

    @Inject
    public TCCollectionInjection(List<CoffeeMaker> coffeeMakers) {
        this.coffeeMakers = coffeeMakers;
    }

    @Inject
    public void injectSet(Set<CoffeeMaker> set) {
        this.coffeeMakerSet = set;
    }
}
