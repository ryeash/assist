package vest.assist.app;

import java.io.IOException;

public interface CoffeeMaker {

    @Timed
        // used by the TimingAspect to validate annotation usage
    String brew();

    default void withParams(Integer i, String str) {
        System.out.println("some arg: " + i + str);
    }

    default void causesError() throws IOException {
        throw new IOException();
    }
}
