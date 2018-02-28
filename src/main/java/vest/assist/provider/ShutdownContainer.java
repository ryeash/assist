package vest.assist.provider;

import vest.assist.InstanceInterceptor;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownContainer implements InstanceInterceptor, AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Set<AutoCloseable> cleanupObjects = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void intercept(Object instance) {
        if (instance instanceof AutoCloseable) {
            synchronized (this) {
                cleanupObjects.add((AutoCloseable) instance);
            }
        }
    }

    @Override
    public int priority() {
        return 2000;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Iterator<AutoCloseable> it = cleanupObjects.iterator();
            while (it.hasNext()) {
                AutoCloseable o = it.next();
                it.remove();
                try {
                    o.close();
                } catch (Throwable e) {
                    // ignored
                }
            }
        }
    }
}
