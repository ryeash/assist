package vest.assist.app;

import javax.inject.Inject;

public class TCStaticInject {

    @Inject
    public static CoffeeMaker globalCoffeeMaker;

    public static boolean methodInjected = false;

    @Inject
    public static void staticInjectMethod() {
        methodInjected = true;
    }
}
