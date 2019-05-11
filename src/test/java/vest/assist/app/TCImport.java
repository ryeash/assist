package vest.assist.app;

import vest.assist.annotations.Factory;
import vest.assist.annotations.Import;

import javax.inject.Named;

public class TCImport {

    @Import(TCImport2.class)
    public static final class TCImport1 {

        @Factory
        @Named("one")
        public CoffeeMaker one() {
            return new FrenchPress();
        }
    }

    @Import({TCImport3.class, TCImport4.class})
    public static final class TCImport2 {

        @Factory
        @Named("two")
        public CoffeeMaker two() {
            return new FrenchPress();
        }
    }

    public static final class TCImport3 {

        @Factory
        @Named("three")
        public CoffeeMaker three() {
            return new FrenchPress();
        }
    }

    public static final class TCImport4 {

        @Factory
        @Named("four")
        public CoffeeMaker four() {
            return new FrenchPress();
        }
    }
}
