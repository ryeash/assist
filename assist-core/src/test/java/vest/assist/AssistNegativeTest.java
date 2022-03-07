package vest.assist;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.assist.annotations.Factory;
import vest.assist.app.TCInvalidClass1;
import vest.assist.app.TCInvalidClass2;
import vest.assist.app.Teapot;

import java.util.List;
import java.util.Set;

public class AssistNegativeTest extends Assert {

    Assist assist;

    @BeforeClass(alwaysRun = true)
    public void initializeAssit() {
        assist = new Assist();
        assist.addConfig("vest.assist.app.AppConfig");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testConfigFailure1() {
        assist.addConfig("tester.no.Thing");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testConfigFailure2() {
        assist.addConfig(Number.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testMissingProvider() {
        assist.providerFor(Set.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDoubleSetProvider() {
        Provider<Teapot> p = assist.providerFor(Teapot.class);
        assist.setSingleton(Teapot.class, p.get());
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void invalidFactory() {
        assist.addConfig(new Object() {
            @Factory
            public void voidFactory() {
            }
        });
    }

    public static class ExceptionMethod {
        @Inject
        public void throwsException() {
            throw new IllegalArgumentException("I'm supposed to do this");
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void injectMethodException() {
        assist.instance(ExceptionMethod.class);
    }

    public static class AbhorrentConstructor {
        @Inject
        public AbhorrentConstructor(List stuff) {

        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void badConstructorArg() {
        assist.instance(AbhorrentConstructor.class);
    }

    @Test
    public void invalidConstructors() {
        Assist assist = new Assist();
//        assist.instance(TCInvalidClass1.class);
        assertThrows(() -> assist.instance(TCInvalidClass1.class));
        assertThrows(() -> assist.instance(TCInvalidClass2.class));
    }
}
