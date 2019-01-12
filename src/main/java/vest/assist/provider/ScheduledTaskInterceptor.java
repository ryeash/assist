package vest.assist.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.Reflector;
import vest.assist.annotations.Scheduled;

import javax.inject.Provider;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
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
    private final Provider<ScheduledExecutorService> lazyExectutor;

    public ScheduledTaskInterceptor(Assist assist) {
        this.assist = assist;
        this.lazyExectutor = new LazyProvider<>(assist, ScheduledExecutorService.class, null);
    }

    @Override
    public void intercept(Object instance) {
        for (Method method : Reflector.of(instance).methods()) {
            Scheduled scheduled = method.getAnnotation(Scheduled.class);
            if (scheduled != null) {
                schedule(scheduled, instance, method);
            }
        }
    }

    private void schedule(Scheduled scheduled, Object instance, Method method) {
        if (scheduled.period() <= 0) {
            throw new RuntimeException("invalid schedule period: must be greater than zero for run type " + scheduled.type() + " on " + Reflector.detailString(method));
        }
        ScheduledExecutorService scheduledExecutorService = lazyExectutor.get();
        ScheduledRunnable runnable = new ScheduledRunnable(instance, method, assist, scheduled);
        long delay = Math.max(0, scheduled.delay());
        ScheduledFuture<?> future;
        switch (scheduled.type()) {
            case FIXED_RATE:
                future = scheduledExecutorService.scheduleAtFixedRate(runnable, delay, scheduled.period(), scheduled.unit());
                break;
            case FIXED_DELAY:
                future = scheduledExecutorService.scheduleWithFixedDelay(runnable, delay, scheduled.period(), scheduled.unit());
                break;
            default:
                throw new RuntimeException("unhandled run type: " + scheduled.type());
        }
        runnable.setFutureHandle(future);
    }

    @Override
    public int priority() {
        return 1100;
    }

    private static final class ScheduledRunnable implements Runnable {
        private final WeakReference<Object> instanceRef;
        private final Method method;
        private final Parameter[] parameters;
        private final Assist assist;
        private final Scheduled scheduled;
        private ScheduledFuture<?> futureHandle;
        private AtomicInteger executionCount;

        ScheduledRunnable(Object instance, Method method, Assist assist, Scheduled scheduled) {
            this.instanceRef = new WeakReference<>(instance);
            this.method = method;
            this.parameters = method.getParameters();
            this.assist = assist;
            this.scheduled = scheduled;
            Reflector.makeAccessible(method);
            this.executionCount = new AtomicInteger(0);
        }

        void setFutureHandle(ScheduledFuture<?> futureHandle) {
            this.futureHandle = futureHandle;
        }

        @Override
        public void run() {
            try {
                Thread.currentThread().setName(scheduled.name());
                Object instance = instanceRef.get();
                if (instance != null) {
                    executionCount.incrementAndGet();
                    method.invoke(instance, assist.getParameterValues(parameters));
                } else if (futureHandle != null) {
                    log.info("object instance for scheduled task [{}] has been garbage collected, canceling task", scheduled.name());
                    futureHandle.cancel(false);
                }
            } catch (Throwable e) {
                log.error("error running scheduled task [{}] [{}]", scheduled.name(), Reflector.detailString(method), e);
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
                            "but the future handle has not been set so it can't be canceled", scheduled.name(), executionCount);
                }
            }
        }

    }
}
