package vest.assist.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.Reflector;
import vest.assist.annotations.Scheduled;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Interceptor that schedules methods annotated with {@link Scheduled} using the default (unqualified)
 * {@link ScheduledExecutorService} that is made available as a provider via the Assist instance.
 * The scheduled tasks that are created under the covers use a WeakReference to the intercepted
 * object instance to allow the instance to be garbage collected. In the case where the reference is GC'd,
 * the scheduled task will be cancelled automatically.
 */
public class ScheduledTaskInterceptor implements InstanceInterceptor {

    private static Logger log = LoggerFactory.getLogger(ScheduledTaskInterceptor.class);

    private final Assist assist;

    public ScheduledTaskInterceptor(Assist assist) {
        this.assist = assist;
    }

    @Override
    public void intercept(Object instance) {
        for (Method method : Reflector.of(instance).methods()) {
            if (method.isAnnotationPresent(Scheduled.class)) {
                schedule(method.getAnnotation(Scheduled.class), instance, method);
            }
        }
    }

    private void schedule(Scheduled scheduled, Object instance, Method method) {
        ScheduledExecutorService scheduledExecutorService = getScheduledExecutorService();
        ScheduledRunnable runnable = new ScheduledRunnable(instance, method, assist, scheduled);
        long delay;
        ScheduledFuture<?> future;
        switch (scheduled.type()) {
            case ONCE:
                if (scheduled.delay() <= 0) {
                    throw new RuntimeException("invalid delay: must be greater than zero for run type " + scheduled.type());
                }
                future = scheduledExecutorService.schedule(runnable, scheduled.delay(), scheduled.unit());
                break;
            case FIXED_RATE:
                delay = Math.max(0, scheduled.delay());
                if (scheduled.period() <= 0) {
                    throw new RuntimeException("invalid period: must be greater than zero for run type " + scheduled.type());
                }
                future = scheduledExecutorService.scheduleAtFixedRate(runnable, delay, scheduled.period(), scheduled.unit());
                break;
            case FIXED_DELAY:
                delay = Math.max(0, scheduled.delay());
                if (scheduled.period() <= 0) {
                    throw new RuntimeException("invalid period: must be greater than zero for run type " + scheduled.type());
                }
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

    private ScheduledExecutorService getScheduledExecutorService() {
        if (assist.hasProvider(ScheduledExecutorService.class)) {
            return assist.instance(ScheduledExecutorService.class);
        } else {
            throw new RuntimeException("no ScheduledExecutorService has been made available via injection");
        }
    }

    private static final class ScheduledRunnable implements Runnable {
        private final WeakReference<Object> instanceRef;
        private final Method method;
        private final Parameter[] parameters;
        private final Assist assist;
        private final Scheduled scheduled;
        private ScheduledFuture<?> futureHandle;

        ScheduledRunnable(Object instance, Method method, Assist assist, Scheduled scheduled) {
            this.instanceRef = new WeakReference<>(instance);
            this.method = method;
            this.parameters = method.getParameters();
            this.assist = assist;
            this.scheduled = scheduled;
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
        }

        void setFutureHandle(ScheduledFuture<?> futureHandle) {
            this.futureHandle = futureHandle;
        }

        @Override
        public void run() {
            try {
                Object instance = instanceRef.get();
                if (instance != null) {
                    method.invoke(instance, assist.getParameterValues(parameters));
                } else if (futureHandle != null) {
                    futureHandle.cancel(false);
                }
            } catch (Throwable e) {
                log.error("error running scheduled task [{}]", scheduled.name(), e);
            }
        }

    }
}
