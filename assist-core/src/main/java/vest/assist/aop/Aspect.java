package vest.assist.aop;

import vest.assist.Prioritized;

/**
 * Base Aspect interface.
 */
public interface Aspect extends Prioritized {
    Object invoke(Invocation invocation) throws Exception;
}
