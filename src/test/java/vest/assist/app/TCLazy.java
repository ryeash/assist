package vest.assist.app;

import vest.assist.annotations.Lazy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class TCLazy {

    @Inject
    @Lazy
    public Provider<CoffeeMaker> lazyMaker;

    @Inject
    @Lazy
    @Named("fancy")
    public Provider<CoffeeMaker> lazyFancyMaker;
}
