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
import vest.assist.provider.AdHocProvider;

import javax.inject.Named;
import javax.inject.Singleton;

public class MiscTest extends Assert {

    private static final Logger log = LoggerFactory.getLogger(MiscTest.class);

    @Test
    public void stringifyTest() {
        Reflector reflector = Reflector.of(TCCollectionInjection.class);

        reflector.fields().stream()
                .limit(1)
                .forEach(f -> log.info(Reflector.detailString(f)));

        reflector.methods().stream()
                .filter(m -> m.getParameterCount() > 0)
                .forEach(m -> {
                    log.info(Reflector.detailString(m));
                    log.info(Reflector.detailString(m.getParameters()[0]));
                });

        log.info(Reflector.detailString(AppConfig.class));
    }

    @Test
    public void scannerTest() {
        assertTrue(PackageScanner.scan("vest.assist.app")
                .anyMatch(c -> c.getSimpleName().equals("CoffeeMaker")));
        assertTrue(PackageScanner.scan("org.slf4j")
                .anyMatch(c -> c.getSimpleName().equals("Logger")));

        assertTrue(PackageScanner.scanClassNames("org.slf4j.impl", ClassLoader.getSystemClassLoader())
                .anyMatch(c -> c.equals("org.slf4j.impl.SimpleLogger")));
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
        log.info("{}", synthetic);
        Named real = PourOver.class.getAnnotation(Named.class);
        assertEquals(synthetic.value(), real.value());
        assertEquals(synthetic, real);
        assertEquals(synthetic.annotationType(), real.annotationType());
    }

    @Test
    public void foo() {
        ProviderIndex idx = new ProviderIndex();
        idx.setProvider(new AdHocProvider<>(new Keurig()));
    }
}
