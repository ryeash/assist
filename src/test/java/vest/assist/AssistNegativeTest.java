package vest.assist;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.assist.annotations.Factory;
import vest.assist.app.Teapot;
import vest.assist.provider.AdHocProvider;

import javax.inject.Provider;
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
        assist.setProvider(Teapot.class, null, new AdHocProvider<>(p.get()));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void invalidFactory() {
        assist.addConfig(new Object() {
            @Factory
            public void voidFactory() {
            }
        });
    }
}
