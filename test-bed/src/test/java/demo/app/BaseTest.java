package demo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import vest.assist.Assist;

import javax.inject.Inject;

public abstract class BaseTest extends Assert {

    private static Assist assist;

    @BeforeSuite(alwaysRun = true)
    public static void setup() {
        if (assist == null) {
            assist = new Assist("demo.app");
        }
    }

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private boolean injected = false;

    @BeforeClass(alwaysRun = true)
    public void injectThis() {
        if (!injected) {
            assist.inject(this);
        }
    }

    @Inject
    private void setInjected() {
        injected = true;
    }
}
