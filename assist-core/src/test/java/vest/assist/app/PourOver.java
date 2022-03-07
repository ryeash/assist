package vest.assist.app;

import jakarta.inject.Named;
import vest.assist.annotations.Aspects;

@Aspects(LoggingAspect.class)
@Named("pourOver")
public class PourOver implements CoffeeMaker {

    @Override
    public String brew() {
        return "pouring over";
    }
}
