package vest.assist.app;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.aop.Aspect;
import vest.assist.aop.Invocation;

@Singleton
public class TimingAspect implements Aspect {

    private static Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Override
    public Object invoke(Invocation invocation) throws Exception {
        if (invocation.method().isAnnotationPresent(Timed.class)) {
            long start = System.nanoTime();
            try {
                return invocation.next() + " timed";
            } finally {
                log.info("ran [{}] in {}ms", invocation, (System.nanoTime() - start) / 1_000_000d);
            }
        } else {
            return invocation.invoke();
        }
    }
}
