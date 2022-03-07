package vest.assist.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.annotations.Scheduled;
import vest.assist.util.MethodTarget;
import vest.assist.util.Reflector;

import java.lang.ref.WeakReference;
import java.lang.reflect.Parameter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor that schedules methods annotated with {@link Scheduled} using the primary (unqualified)
 * {@link ScheduledExecutorService} that is made available as a provider via the Assist instance.
 * The scheduled tasks that are created under the covers use a WeakReference to the intercepted
 * object instance to allow the instance to be garbage collected. In the case where the reference is GC'd,
 * the scheduled task will be cancelled automatically.
 */
public class ScheduledTaskInterceptor implements InstanceInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskInterceptor.class);

    private final Assist assist;

    public ScheduledTaskInterceptor(Assist assist) {
        this.assist = assist;
    }

    @Override
    public void intercept(Object instance) {
        for (MethodTarget method : Reflector.of(instance).methods()) {
            Scheduled scheduled = method.getAnnotation(Scheduled.class);
            if (scheduled != null) {
                schedule(scheduled, instance, method);
            }
        }
    }

    private void schedule(Scheduled scheduled, Object instance, MethodTarget method) {
        if (scheduled.period() <= 0) {
            throw new RuntimeException("invalid schedule period: must be greater than zero for run type " + scheduled.type() + " on " + Reflector.detailString(method));
        }
        ScheduledExecutorService scheduledExecutorService = getExecutor(scheduled);
        ScheduledRunnable runnable = new ScheduledRunnable(instance, method, assist, scheduled);
        long delay = Math.max(0, scheduled.delay());
        ScheduledFuture<?> future = switch (scheduled.type()) {
            case FIXED_RATE -> scheduledExecutorService.scheduleAtFixedRate(runnable, delay, scheduled.period(), scheduled.unit());
            case FIXED_DELAY -> scheduledExecutorService.scheduleWithFixedDelay(runnable, delay, scheduled.period(), scheduled.unit());
        };
        runnable.setFutureHandle(future);
    }

    private ScheduledExecutorService getExecutor(Scheduled scheduled) {
        if (scheduled.scheduler().equals(Scheduled.UNSET)) {
            return assist.instance(ScheduledExecutorService.class);
        } else {
            return assist.instance(ScheduledExecutorService.class, scheduled.scheduler());
        }
    }

    @Override
    public int priority() {
        return 1100;
    }

    private static final class ScheduledRunnable implements Runnable {
        private final WeakReference<Object> instanceRef;
        private final MethodTarget method;
        private final Parameter[] parameters;
        private final Assist assist;
        private final Scheduled scheduled;
        private final AtomicInteger executionCount;
        private ScheduledFuture<?> futureHandle;

        ScheduledRunnable(Object instance, MethodTarget method, Assist assist, Scheduled scheduled) {
            this.instanceRef = new WeakReference<>(instance);
            this.method = method;
            this.parameters = method.getParameters();
            this.assist = assist;
            this.scheduled = scheduled;
            this.executionCount = new AtomicInteger(0);
        }

        void setFutureHandle(ScheduledFuture<?> futureHandle) {
            this.futureHandle = futureHandle;
        }

        private String taskName() {
            return scheduled.name().isEmpty() ? "<unknown>" : scheduled.name();
        }

        private String threadName() {
            if (scheduled.name().isEmpty()) {
                return Thread.currentThread().getName();
            } else {
                return scheduled.name();
            }
        }

        @Override
        public void run() {
            try {
                Object instance = instanceRef.get();
                if (instance != null) {
                    executionCount.incrementAndGet();
                    String originalThreadName = Thread.currentThread().getName();
                    try {
                        Thread.currentThread().setName(threadName());
                        method.invoke(instance, assist.getParameterValues(parameters));
                    } finally {
                        Thread.currentThread().setName(originalThreadName);
                    }
                } else if (futureHandle != null) {
                    log.info("object instance for scheduled task [{}] [{}] has been garbage collected, canceling task", taskName(), Reflector.detailString(method));
                    futureHandle.cancel(false);
                }
            } catch (Throwable e) {
                log.error("error running scheduled task [{}] [{}]", taskName(), Reflector.detailString(method), e);
            } finally {
                cancelIfExecutionLimitReached();
            }
        }

        private void cancelIfExecutionLimitReached() {
            if (scheduled.executions() < 0) {
                return;
            }
            if (executionCount.get() >= scheduled.executions()) {
                if (futureHandle != null) {
                    futureHandle.cancel(false);
                } else {
                    log.warn("scheduled task {} has hit its execution limit of {}, " +
                            "but the future handle has not been set so it can't be canceled", taskName(), executionCount);
                }
            }
        }

    }
}
