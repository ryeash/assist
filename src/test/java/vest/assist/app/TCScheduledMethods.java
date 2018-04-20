package vest.assist.app;

import vest.assist.annotations.Scheduled;

import static vest.assist.annotations.Scheduled.RunType.*;

public class TCScheduledMethods {

    public int runOnceCount = 0;
    public int fixedDelayCount = 0;
    public int fixedRateCount = 0;

    @Scheduled(type = ONCE, delay = 10)
    private void scheduledOnceMethod() {
        runOnceCount++;
    }

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
}
