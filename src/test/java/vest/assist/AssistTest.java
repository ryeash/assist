package vest.assist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.assist.app.Child;
import vest.assist.app.CoffeeMaker;
import vest.assist.app.Coosie;
import vest.assist.app.DAO1;
import vest.assist.app.DAO2;
import vest.assist.app.FrenchPress;
import vest.assist.app.Keurig;
import vest.assist.app.Leather;
import vest.assist.app.Log;
import vest.assist.app.TCMultipleDependenciesSatisfied;
import vest.assist.app.Parent;
import vest.assist.app.PourOver;
import vest.assist.app.ScannedComponent;
import vest.assist.app.TCCollectionInjection;
import vest.assist.app.TCPropertyInjection;
import vest.assist.app.TCResourceInjection;
import vest.assist.app.TCScheduledMethods;
import vest.assist.app.Teapot;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AssistTest extends Assert {

    private static final Logger log = LoggerFactory.getLogger(AssistTest.class);

    public void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //ignored
        }
    }

    Assist assist;

    @BeforeClass(alwaysRun = true)
    public void initializeAssist() {
        assist = new Assist();
        assist.addValueLookup((rawType, genericType, annotatedElement) -> (rawType == Logger.class) ? LoggerFactory.getLogger(((Field) annotatedElement).getDeclaringClass()) : null);
        assist.addConfig("vest.assist.app.AppConfig");
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() {
        assist.close();
    }

    @Test
    public void testInit() {
        log.info("{}", assist);
        assertEquals(assist.instance(Assist.class), assist);
    }

    @Test
    public void testBasicMethodProvider() {
        InputStream is = assist.instance(InputStream.class.getCanonicalName());
        assertNotNull(is);
        assertEquals(is.getClass(), ByteArrayInputStream.class);
    }

    @Test
    public void testBasicConstructorProvider() {
        List<Teapot> list = IntStream.range(0, 10)
                .parallel()
                .mapToObj(i -> assist.instance(Teapot.class))
                .collect(Collectors.toList());
        for (int i = 1; i < list.size(); i++) {
            assertNotNull(list.get(i));
            assertEquals(list.get(0), list.get(i));
        }
    }

    @Test
    public void testScanner() {
        PackageScanner.scan("vst.assist.app")
                .peek(type -> assertTrue(type.getCanonicalName().startsWith("vst.assist.app")))
                .forEach(type -> log.info("{}", type));
    }

    @Test
    public void testScannedDep() {
        assertTrue(assist.hasProvider(ScannedComponent.class));
    }

    @Test
    public void testProvidersFor() {
        List<Provider<InputStream>> iss = assist.providersFor(InputStream.class)
                .peek(p -> log.info("{}", p))
                .collect(Collectors.toList());
        assertEquals(iss.size(), 2);
    }

    @Test
    public void testHasProvider() {
        assertTrue(assist.hasProvider(CoffeeMaker.class));
        assertTrue(assist.hasProvider(Closeable.class));
        // only qualified provider for Coosie exists, so should be false
        assertFalse(assist.hasProvider(Coosie.class));

        assertTrue(assist.hasProvider(CoffeeMaker.class, "keurig"));
        assertFalse(assist.hasProvider(CoffeeMaker.class, "smash"));
    }

    @Test
    public void testScopeHandling() {
        // Singleton
        Teapot teapot1 = assist.providerFor(Teapot.class).get();
        Teapot teapot2 = assist.providerFor(Teapot.class).get();
        assertEquals(teapot1, teapot2);

        // custom ThreadLocal scope
        assertEquals(assist.instance(CoffeeMaker.class), assist.instance(CoffeeMaker.class));

        AtomicReference<CoffeeMaker> cm1 = new AtomicReference<>();
        new Thread(() -> cm1.set(assist.instance(CoffeeMaker.class))).start();
        AtomicReference<CoffeeMaker> cm2 = new AtomicReference<>();
        new Thread(() -> cm2.set(assist.instance(CoffeeMaker.class))).start();
        delay(100);
        assertNotNull(cm1.get());
        assertNotNull(cm2.get());
        assertNotEquals(cm1.get(), cm2.get());
    }

    @Inject
    @Named("keurig")
    protected CoffeeMaker coffeeMaker;

    @Inject
    @Leather(color = Leather.Color.BLACK)
    private Coosie bl;

    @Inject
    @Leather(color = Leather.Color.RED)
    Provider<Coosie> rl;


    @Test
    public void testAdHocInjection() {
        assist.inject(this);
        assertNotNull(coffeeMaker);
        assertEquals(coffeeMaker.getClass(), Keurig.class);

        assertEquals(bl.id(), "black");
        assertEquals(rl.get().id(), "red");
    }

    @Test
    public void testGetProviderByName() {
        CoffeeMaker fp = assist.providerFor(CoffeeMaker.class, "frenchPress").get();
        fp.brew();
        log.info("{}", fp.getClass());
        log.info("{}", fp);
        assertTrue(fp instanceof FrenchPress);
    }

    @Test
    public void testInjectMethodOrder() {
        Parent p = assist.instance(Parent.class);
        assertEquals(p.getI(), 1);
        assertEquals(p.over, "parent");
        assertEquals(p.noInject, "parent");
        log.info("------------------------");
        Child c = assist.instance(Child.class);
        assertEquals(c.getI(), -1);
        assertEquals(c.over, "child");
        assertNull(c.noInject);
    }

    @Test
    public void testSettingImplementation() {
        Assist assist = new Assist();
        assist.addValueLookup((rawType, genericType, annotatedElement) -> annotatedElement.isAnnotationPresent(Log.class) ? LoggerFactory.getLogger(rawType) : null);
        assist.addImplementingClass(CoffeeMaker.class, Keurig.class);
        log.info("{}", assist);

        assertTrue(assist.hasProvider(CoffeeMaker.class, new NamedImpl("k")));
        CoffeeMaker cm = assist.instance(CoffeeMaker.class, "k");
        assertEquals(cm.getClass(), Keurig.class);

        List<Provider<CoffeeMaker>> list = assist.providersFor(CoffeeMaker.class).collect(Collectors.toList());
        assertEquals(list.size(), 1);


        assist.addImplementingClass(CoffeeMaker.class, FrenchPress.class);
        log.info("{}", assist);

        assertEquals(assist.instance(CoffeeMaker.class).getClass(), FrenchPress.class);
        assertEquals(assist.instance(CoffeeMaker.class, "k").getClass(), Keurig.class);

        list = assist.providersFor(CoffeeMaker.class).collect(Collectors.toList());
        assertEquals(list.size(), 2);

        log.info("{}", assist);
    }


    @Test
    public void testThreading() {
        Assist assist = new Assist();
        assist.addValueLookup((rawType, genericType, annotatedElement) -> annotatedElement.isAnnotationPresent(Log.class) ? LoggerFactory.getLogger(rawType) : null);

        // should only be ONE singleton class ever created
        List<Integer> list = IntStream.range(0, 17)
                .parallel()
                .mapToObj(i -> assist.instance(ScannedComponent.class))
                .map(Object::hashCode)
                .collect(Collectors.toList());
        log.info("{}", list);
        Integer first = list.get(0);
        for (Integer hash : list) {
            assertEquals(hash, first);
        }

        // no repeats for non-singleton classes
        list = IntStream.range(0, 16)
                .parallel()
                .mapToObj(i -> assist.instance(FrenchPress.class))
                .map(Object::hashCode)
                .collect(Collectors.toList());
        log.info("{}", list);
        first = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            assertNotEquals(list.get(i), first);
        }
    }

    @Test
    public void getNamedSuperclassForProvider() {
        InputStream cm = assist.instance(InputStream.class, "bais");
        assertTrue(cm instanceof ByteArrayInputStream);
    }

    @Test
    public void aspects() {
        CoffeeMaker aspect = assist.instance(CoffeeMaker.class, "aspect1");
        String result = aspect.brew();
        assertEquals(result, "french");
        aspect.withParams(2, "hope");
        assertThrows(IOException.class, aspect::causesError);

        CoffeeMaker aspect2 = assist.instance(CoffeeMaker.class, "aspect2");
        result = aspect2.brew();
        assertEquals(result, "french timed");

        // make sure scoping still works
        CoffeeMaker aspectAgain = assist.instance(CoffeeMaker.class, "aspect2");
        assertSame(aspect2, aspectAgain);
        result = aspectAgain.brew();
        assertEquals(result, "french timed");

        assist.addImplementingClass(CoffeeMaker.class, PourOver.class);
        CoffeeMaker po = assist.instance(CoffeeMaker.class, "pourOver");
        log.info("{}", po.brew());
    }

    @Test
    public void boot() {
        Assist.main(new String[]{"vest.assist.app.BootConfig", "-e", "extra", "--debug", "--withValue=something"});
    }

    @Test
    public void instances() {
        assist.instances(CoffeeMaker.class)
                .forEach(cm -> log.info("{}", cm.getClass().getSimpleName()));
    }

    @Test
    public void wiringListsAndSets() {
        TCCollectionInjection man = assist.instance(TCCollectionInjection.class);
        assertNotNull(man.coffeeMakers);
        assertFalse(man.coffeeMakers.isEmpty());

        assertNotNull(man.coffeeMakerSet);
        assertFalse(man.coffeeMakerSet.isEmpty());

        assertNotNull(man.coffeeMakerCollection);
        assertFalse(man.coffeeMakerCollection.isEmpty());

        CoffeeMaker cm = assist.instance(CoffeeMaker.class);
        assertTrue(man.coffeeMakers.contains(cm));
        assertTrue(man.coffeeMakerSet.contains(cm));
        assertTrue(man.coffeeMakerCollection.contains(cm));
    }

    @Test
    public void multipleDependenciesSatisfiedByOneClass() {
        DAO1 dao1 = assist.instance(DAO1.class);
        assertNotNull(dao1);
        assertSame(dao1.getClass(), TCMultipleDependenciesSatisfied.class);

        DAO2 dao2 = assist.instance(DAO2.class);
        assertNotNull(dao2);
        assertSame(dao2.getClass(), TCMultipleDependenciesSatisfied.class);
    }

    @Test
    public void resourceInjectionTest() {
        TCResourceInjection instance = assist.instance(TCResourceInjection.class);
        assertNotNull(instance.fieldInjected);
    }

    @Test
    public void scheduledTest() {
        TCScheduledMethods sched = assist.instance(TCScheduledMethods.class);
        delay(500);
        assertEquals(sched.runOnceCount, 1);
        assertEquals(sched.fixedDelayCount, 10);
        assertEquals(sched.fixedRateCount, 10);
    }

    @Test
    public void propertiesTest(){
        TCPropertyInjection prop = assist.instance(TCPropertyInjection.class);
        assertEquals(prop.getStr(), "value");
        assertEquals(prop.bool, Boolean.TRUE);
        assertEquals(prop.integer, 12);
        assertEquals(prop.numbers, Arrays.asList(1D,1D,2D,3D,5D,8D,13D));
        assertEquals(prop.demoEnum, ConfigurationTest.DemoEnum.CHARLIE);
    }
}
