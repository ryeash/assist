package vest.assist.app;

import jakarta.inject.Inject;

public class TCStaticInject {

    public static boolean methodInjected = false;

    @Inject
    public static void staticInjectMethod() {
        methodInjected = true;
    }
}
