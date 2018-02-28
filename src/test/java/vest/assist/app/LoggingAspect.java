package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.aop.Aspect;
import vest.assist.aop.Invocation;

public class LoggingAspect extends Aspect {

    private Logger log;

    @Override
    public void init(Object instance) {
        super.init(instance);
        this.log = LoggerFactory.getLogger(instance.getClass());
    }

    @Override
    public void pre(Invocation invocation) throws Throwable {
        log.info("entering {}", invocation);
    }

    @Override
    public void post(Invocation invocation) throws Throwable {
        log.info("exiting {}", invocation);
    }
}
