package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.InstanceInterceptor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the {@link InstanceInterceptor} that tracks closable objects in a {@link WeakHashMap}
 * so that they can be closed when {@link Assist#close()} is called.
 */
public class ShutdownContainer implements InstanceInterceptor, AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Set<AutoCloseable> cleanupObjects = Collections.newSetFromMap(new WeakHashMap<>(32));

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
            cleanupObjects.stream()
                    .parallel()
                    .forEach(ShutdownContainer::closeQuietly);
            cleanupObjects.clear();
        }
    }

    private static void closeQuietly(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Throwable t) {
                // ignored
            }
        }
    }
}
