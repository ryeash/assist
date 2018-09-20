package vest.assist.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Marks a method in an injected class to be scheduled for execution
 * via a {@link java.util.concurrent.ScheduledExecutorService}. In order
 * to use this annotation, a ScheduledExecutorService must be made available
 * as via the Assist instance performing the injection.
 * Depending on the run type either the delay or period are required, see
 * {@link RunType} for more information.
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {

    /**
     * Assigns a name to the scheduled task. The name will be used for any internal logging (e.g. uncaught exceptions)
     * related to the scheduled method.
     */
    String name() default "scheduled-task";

    /**
     * The delay before first execution. A negative value indicates no delay.
     */
    long delay() default -1;

    /**
     * The target period between executions of the method. This must be greater than 0.
     */
    long period();

    /**
     * The unit to use for the delay and period values.
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * The {@link RunType} to use for this scheduled method.
     */
    RunType type() default RunType.FIXED_RATE;

    /**
     * The number of times the scheduled method will be executed. Defaults to -1, which is interpreted as unlimited.
     */
    int executions() default -1;

    enum RunType {
        /**
         * Run the method repeatedly with configured delay and period.
         * Requires the period attribute be greater than 0.
         * Maps to {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}
         */
        FIXED_RATE,

        /**
         * Run the method repeatedly with the configured delay and period.
         * Requires the period attribute be greater than 0.
         * Maps to {@link java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
         */
        FIXED_DELAY
    }
}
