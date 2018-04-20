package vest.assist.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.Assist;
import vest.assist.InstanceInterceptor;
import vest.assist.Reflector;
import vest.assist.annotations.Scheduled;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Interceptor that schedules methods annotated with {@link Scheduled} using the
 * {@link ScheduledExecutorService} that is made available as a provider via the Assist instance.
 */
public class ScheduledTaskInterceptor implements InstanceInterceptor {

    private static Logger log = LoggerFactory.getLogger(ScheduledTaskInterceptor.class);

    private final Assist assist;

    public ScheduledTaskInterceptor(Assist assist) {
        this.assist = assist;
    }

    @Override
    public void intercept(Object instance) {
        Reflector.of(instance).forAnnotatedMethods(Scheduled.class, (st, method) -> {
            ScheduledExecutorService scheduledExecutorService = getScheduledExecutorService();
            Runnable r = toRunnable(instance, method);
            long delay;
            switch (st.type()) {
                case ONCE:
                    if (st.delay() < 0) {
                        throw new RuntimeException("invalid delay: must be greater than or equal to zero");
                    }
                    scheduledExecutorService.schedule(r, st.delay(), st.unit());
                    break;
                case FIXED_RATE:
                    delay = Math.max(0, st.delay());
                    if (st.period() <= 0) {
                        throw new RuntimeException("invalid period: must be greater than zero");
                    }
                    scheduledExecutorService.scheduleAtFixedRate(r, delay, st.period(), st.unit());
                    break;
                case FIXED_DELAY:
                    delay = Math.max(0, st.delay());
                    if (st.period() <= 0) {
                        throw new RuntimeException("invalid period: must be greater than zero");
                    }
                    scheduledExecutorService.scheduleWithFixedDelay(r, delay, st.period(), st.unit());
                    break;
                default:
                    throw new RuntimeException("unhandled run type: " + st.type());
            }
        });
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

    private Runnable toRunnable(Object o, Method method) {
        return () -> {
            try {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(o, assist.getParameterValues(method.getParameters()));
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("error running scheduled task", e);
            }
        };
    }
}
