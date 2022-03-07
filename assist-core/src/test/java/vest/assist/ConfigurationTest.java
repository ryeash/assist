package vest.assist;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.assist.conf.Builder;
import vest.assist.conf.ConfigurationFacade;
import vest.assist.conf.DefaultConfigurationFacade;
import vest.assist.conf.EnvironmentFacade;
import vest.assist.conf.InterpolationWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConfigurationTest extends Assert {

    public enum DemoEnum {
        ALPHA, BRAVO, CHARLIE
    }

    public static class ValueHolder {
        String value;

        public ValueHolder(String value) {
            this.value = value;
        }
    }

    protected String testFile;

    @BeforeClass(alwaysRun = true)
    public void findFile() throws IOException {
        this.testFile = Files.find(new File(".").toPath(), 3, (path, basicFileAttributes) -> path.getFileName().toString().equals("test.conf"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("error finding test config"))
                .toString();
    }

    @Test
    public void facadeTest() {
        ConfigurationFacade conf = Builder.defaultFacade(testFile);
        System.out.println(conf);

        assertEquals(conf.get("string"), "value");
        assertEquals(conf.get("missing", "miss"), "miss");
        assertEquals(conf.get("integer", Integer.class), Integer.valueOf(12));
        assertEquals(conf.get("integer", 0, Integer::valueOf), Integer.valueOf(12));
        assertEquals(conf.get("missingInteger", 0, Integer::valueOf), Integer.valueOf(0));

        assertEquals(conf.get("double", Double.class), 43.231);
        assertEquals(conf.get("boolean", Boolean.class), Boolean.TRUE);
        assertEquals(conf.get("enum", DemoEnum.class), DemoEnum.CHARLIE);

        assertEquals(conf.getList("string.list"), Arrays.asList("one", "two", "three"));
        assertEquals(conf.getSet("string.list"), new HashSet<>(Arrays.asList("one", "two", "three")));

        assertEquals(conf.getList("missing.list", Arrays.asList("a", "b")), Arrays.asList("a", "b"));
        assertEquals(conf.getSet("missing.set", new HashSet<>(Arrays.asList("a", "b"))), new HashSet<>(Arrays.asList("a", "b")));
        assertEquals(conf.getList("list.empty"), Arrays.asList("alpha", "bravo", "delta"));

        assertEquals(conf.getList("numbers.list", Integer.class), Arrays.asList(1, 1, 2, 3, 5, 8, 13));
        assertEquals(conf.getSet("numbers.list", Integer.class), new HashSet<>(Arrays.asList(1, 2, 3, 5, 8, 13)));
        assertEquals(conf.getList("enum.list", DemoEnum.class), Arrays.asList(DemoEnum.ALPHA, DemoEnum.BRAVO));

        assertTrue(conf.getStream("numbers.list", Integer.class)
                .findFirst()
                .isPresent());

        ValueHolder holder = conf.get("string", ValueHolder.class);
        assertEquals(holder.value, "value");

        assertEquals(conf.get("double", (Class<?>) null), "43.231");


        Properties properties = conf.toProperties();
        assertEquals(properties.get("string"), "value");
        assertEquals(properties.getProperty("string"), "value");
        assertEquals(properties.getProperty("missing", "fallback"), "fallback");
    }

    @Test
    public void loadFromClasspath() {
        ConfigurationFacade conf = ConfigurationFacade.build()
                .classpathFile("test.conf")
                .finish();
        assertEquals(conf.get("string"), "value");
    }

    @Test
    public void macroTest() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "value");
        map.put("interpolate", "value ${name} value");
        map.put("double.interpolate", "value ${interpolate} value");
        map.put("beginning", "${name} beginning");
        map.put("end", "end ${name}");
        map.put("missing", "missing ${link-to-nowhere}");

        ConfigurationFacade facade = Builder.start()
                .map(map)
                .finish();

        InterpolationWrapper macro = new InterpolationWrapper(facade, "${", "}");
        assertEquals(macro.get("name"), "value");
        assertEquals(macro.get("interpolate"), "value value value");
        assertEquals(macro.get("double.interpolate"), "value value value value value");
        assertEquals(macro.get("beginning"), "value beginning");
        assertEquals(macro.get("end"), "end value");
        assertEquals(macro.get("missing"), "missing null");
    }

    @Test
    public void environmentTest() {
        Map<String, String> map = new HashMap<>();
        map.put("environment", "dev");
        map.put("dev.name", "dev-value");
        map.put("test.name", "test-value");
        map.put("name", "default-value");

        ConfigurationFacade facade = Builder.start()
                .map(map)
                .finish();

        EnvironmentFacade env = new EnvironmentFacade(facade);
        assertEquals(env.get("name"), "dev-value");

        map.put("environment", "test");
        env.reload();
        assertEquals(env.get("name"), "test-value");

        map.put("environment", null);
        env.reload();
        assertEquals(env.get("name"), "default-value");
    }

    @Test
    public void cachingTest() {
        Map<String, String> map = new HashMap<>();

        ConfigurationFacade facade = Builder.start()
                .map(map)
                .file(testFile)
                .enableCaching()
                .finish();

        System.out.println(facade);

        assertEquals(facade.get("string"), "value");
        map.put("string", "different value");
        assertEquals(facade.get("string"), "value");
        facade.reload();
        assertEquals(facade.get("string"), "different value");

        assertSame(facade.get("integer"), facade.get("integer"));
        assertSame(facade.getList("string.list"), facade.getList("string.list"));

        List<String> list = facade.getList("string.list");
        facade.reload();
        assertNotSame(facade.getList("string.list"), list);
    }

    @Test
    public void changingDelimiter() {
        Map<String, String> map = new HashMap<>();
        map.put("list1", "a,b,c,d,e,f");
        map.put("list2", "a|b|c|d|e|f");

        DefaultConfigurationFacade conf = new DefaultConfigurationFacade(Collections.singletonList(map::get));
        conf.setListDelimiter(',');

        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", "f");
        assertEquals(conf.getList("list1"), expected);

        conf.setListDelimiter('|');
        assertEquals(conf.getList("list2"), expected);
    }

    @Test
    public void traceTest() {
        Map<String, String> vals = new HashMap<>();
        vals.put("string", "the other value");

        ConfigurationFacade conf = ConfigurationFacade.build()
                .environment()
                .system()
                .file(testFile)
                .map(vals)
                .finish();
        for (String trace : conf.trace("string")) {
            System.out.println(trace);
        }
    }

    @Test
    public void structured() throws IOException {
        String testFile = Files.find(new File(".").toPath(), 3, (path, basicFileAttributes) -> path.getFileName().toString().equals("structured.conf"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("error finding test config"))
                .toString();

        ConfigurationFacade c = Builder.start()
                .structured(testFile)
                .enableInterpolation()
                .enableEnvironments()
                .finish();

        System.out.println(c);

        assertEquals(c.get("macro"), "true");
        assertEquals(c.get("basic.number", Integer.class), Integer.valueOf(42));
        assertEquals(c.get("basic.nested.macro"), "null");
        assertEquals(c.get("sameline1"), "one");
        assertEquals(c.get("sameline2"), "two");
    }
}
