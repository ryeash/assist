package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.aop.Aspect;
import vest.assist.aop.Invocation;

import java.util.HashSet;
import java.util.Set;

public class LoggingAspect implements Aspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private final Set<Invocation> alreadyCalled = new HashSet<>(64);

    @Override
    public Object invoke(Invocation invocation) throws Exception {
        boolean added = alreadyCalled.add(invocation);
        log.info("entering {}", invocation);
        log.info("arguments {}", invocation.arity());
        log.info("args {}", invocation.args());
        log.info("instance {}", invocation.instance());
        if (invocation.arity() > 0) {
            log.info("arg0 {}", invocation.arg(0));
        }
        if (!added) {
            log.info("method has been called before in the same way");
        }
        Object result = invocation.next();
        log.info("exiting {}", invocation);
        return result;
    }
}
