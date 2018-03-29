package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.aop.Aspect;
import vest.assist.aop.Invocation;

import javax.inject.Singleton;

@Singleton
public class TimingAspect extends Aspect {

    private static Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Override
    public void exec(Invocation invocation) throws Throwable {
        if (invocation.getMethod().isAnnotationPresent(Timed.class)) {
            long start = System.nanoTime();
            try {
                super.exec(invocation);
                invocation.setResult(invocation.getResult() + " timed");
            } finally {
                log.info("ran [{}] in {}ms", invocation, (System.nanoTime() - start) / 1_000_000d);
            }
        } else {
            super.exec(invocation);
        }
    }
}
