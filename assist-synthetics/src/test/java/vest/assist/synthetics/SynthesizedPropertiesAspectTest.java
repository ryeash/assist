package vest.assist.synthetics;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import vest.assist.Assist;
import vest.assist.synthetics.app.AppProperties;

import java.util.Arrays;

public class SynthesizedPropertiesAspectTest {

    public static Assist assist;

    @BeforeSuite(alwaysRun = true)
    public static void initialize() {
        if (assist == null) {
            assist = new Assist("vest.assist.synthetics");
        }
    }

    @Test
    public void synthesizedPropertiesTest() {
        AppProperties instance = assist.instance(AppProperties.class);
        Assert.assertEquals(instance.stringProp(), "value");
        Assert.assertEquals(instance.numbers(), Arrays.asList(1, 1, 2, 3, 5, 8, 13));
        System.out.println(instance.nothing());
    }
}
