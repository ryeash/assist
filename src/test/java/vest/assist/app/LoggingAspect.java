package vest.assist.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.assist.aop.AfterMethod;
import vest.assist.aop.BeforeMethod;
import vest.assist.aop.Invocation;

import java.util.HashSet;
import java.util.Set;

public class LoggingAspect implements BeforeMethod, AfterMethod {

    private Logger log;

    private Set<Invocation> alreadyCalled = new HashSet<>();

    @Override
    public void init(Object instance) {
        this.log = LoggerFactory.getLogger(instance.getClass());
    }

    @Override
    public void before(Invocation invocation) {
        boolean added = alreadyCalled.add(invocation);
        log.info("entering {}", invocation);
        log.info("arguments {}", invocation.getArgCount());
        log.info("args {}", invocation.getArgs());
        log.info("instance {}", invocation.getInstance());
        invocation.setArgs(invocation.getArgs());
        Assert.assertThrows(IllegalArgumentException.class, () -> invocation.setArgs(1, 2, 3, 4, 5));
        invocation.setInstance(invocation.getInstance());
        invocation.setMethod(invocation.getMethod());
        if (invocation.getArgCount() > 0) {
            log.info("arg0 {}", invocation.getArgs()[0]);
        }
        if (!added) {
            log.info("method has been called before in the same way");
        }
    }

    @Override
    public Object after(Invocation invocation, Object result) {
        log.info("exiting {}", invocation);
        return result;
    }

}
