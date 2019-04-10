package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.assist.annotations.Scheduled;

import static vest.assist.annotations.Scheduled.RunType.FIXED_DELAY;
import static vest.assist.annotations.Scheduled.RunType.FIXED_RATE;

public class TCScheduledMethods {

    public static Logger log = LoggerFactory.getLogger(TCScheduledMethods.class);

    public int fixedDelayCount = 0;
    public int fixedRateCount = 0;
    public int limitedExecutions = 0;

    @Scheduled(type = FIXED_RATE, period = 50)
    private void scheduledFixedRateMethod() {
        fixedRateCount++;
    }

    @Scheduled(type = FIXED_DELAY, period = 1)
    private void scheduledFixedDelayMethod() {
        fixedDelayCount++;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // ignored
        }
    }

    @Scheduled(type = FIXED_RATE, period = 3, executions = 4)
    private void scheduledLimitedExecutions(CoffeeMaker cm) {
        Assert.assertNotNull(cm);
        limitedExecutions++;

    }
}
