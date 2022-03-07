package vest.assist.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import vest.assist.Assist;

import java.util.Objects;

/**
 * Base test class for validating Assist based applications.
 * Automatically initializes an instance of Assist based on the {@link TestConfiguration}
 * annotation (which is required).
 */
public abstract class AssistBaseTest extends Assert {

    private static final AssistManager assistManager = new AssistManager();

    private Assist assist;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @BeforeClass(alwaysRun = true)
    public void setupAssist() {
        TestConfiguration testConfiguration = getClass().getAnnotation(TestConfiguration.class);
        Objects.requireNonNull(testConfiguration, "extensions of " + getClass().getSimpleName() + " must have a @TestConfiguration annotation");
        this.assist = assistManager.getOrCreate(testConfiguration);
    }

    @AfterSuite(alwaysRun = true)
    public void teardownAssist() {
        assistManager.close();
    }

    /**
     * Get the assist instance that was initialized (or pulled from cache) for this test class.
     */
    protected Assist assist() {
        return Objects.requireNonNull(assist, "this class was not instantiated correctly");
    }
}
