package vest.assist.app;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;

// TestCase: can inject Lists, Sets, and Collections (constructor, field, and method)
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

}
