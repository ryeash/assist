package vest.assist.app;

import vest.assist.annotations.Lazy;

import javax.inject.Named;
import javax.inject.Provider;

public class TCLazy {

    @Lazy
    public Provider<CoffeeMaker> lazyMaker;

    @Lazy
    @Named("fancy")
    public Provider<CoffeeMaker> lazyFancyMaker;
}
