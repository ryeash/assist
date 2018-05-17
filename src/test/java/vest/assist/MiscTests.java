package vest.assist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import vest.assist.app.AppConfig;
import vest.assist.app.Keurig;
import vest.assist.app.PourOver;
import vest.assist.app.TCCollectionInjection;
import vest.assist.app.Teapot;

import javax.inject.Named;
import javax.inject.Singleton;

public class MiscTests extends Assert {

    private static final Logger log = LoggerFactory.getLogger(MiscTests.class);

    @Test
    public void stringifyTest() {
        Reflector reflector = Reflector.of(TCCollectionInjection.class);

        reflector.fields().stream()
                .limit(1)
                .forEach(f -> log.info(ProviderTypeValueLookup.detailString(f)));

        reflector.methods().stream()
                .limit(1)
                .forEach(m -> {
                    log.info(ProviderTypeValueLookup.detailString(m));
                    log.info(ProviderTypeValueLookup.detailString(m.getParameters()[0]));
                });

        log.info(ProviderTypeValueLookup.detailString(AppConfig.class));
    }

    @Test
    public void scannerTest() {
        assertTrue(PackageScanner.scan("vest.assist.app")
                .anyMatch(c -> c.getSimpleName().equals("CoffeeMaker")));
        assertTrue(PackageScanner.scan("org.slf4j")
                .anyMatch(c -> c.getSimpleName().equals("Logger")));
    }

    @Test
    public void reflectorTest() {
        Reflector r = Reflector.of(AppConfig.class);
        assertEquals(r.type(), AppConfig.class);
        assertEquals(Reflector.of(Teapot.class).scope().annotationType(), Singleton.class);
        assertEquals(Reflector.of(Keurig.class).qualifier().annotationType(), Named.class);
        assertNull(r.scope());
        log.info(r.toString());
        assertEquals(Reflector.of(AppConfig.class), r);
        Reflector.clear();
        assertNotEquals(Reflector.of(AppConfig.class), r);
    }

    @Test
    public void namedImplTest() {
        Named synthetic = new NamedImpl("pourOver");
        Named real = PourOver.class.getAnnotation(Named.class);
        assertEquals(synthetic.value(), real.value());
        assertEquals(synthetic, real);
        assertEquals(synthetic.toString(), real.toString());
        assertEquals(synthetic.annotationType(), real.annotationType());
    }
}
