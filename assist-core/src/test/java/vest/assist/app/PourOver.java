package vest.assist.app;

import vest.assist.annotations.Aspects;

import javax.inject.Named;

@Aspects(LoggingAspect.class)
@Named("pourOver")
public class PourOver implements CoffeeMaker {

    @Override
    public String brew() {
        return "pouring over";
    }
}
