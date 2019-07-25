package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.aop.Invocation;
import vest.assist.aop.InvokeMethod;

import javax.inject.Singleton;

@Singleton
public class TimingAspect implements InvokeMethod {

    private static Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Override
    public Object invoke(Invocation invocation) throws Throwable {
        if (invocation.getMethod().isAnnotationPresent(Timed.class)) {
            long start = System.nanoTime();
            try {
                return invocation.invoke() + " timed";
            } finally {
                log.info("ran [{}] in {}ms", invocation, (System.nanoTime() - start) / 1_000_000d);
            }
        } else {
            return invocation.invoke();
        }
    }
}
