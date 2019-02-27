package vest.assist.event;

import vest.assist.Reflector;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * A simple event bus, listeners are registered and receive compatible messages published to the bus.
 * Useful for decoupling code within a JVM.
 */
public class EventBus {
    private final ExecutorService executorService;
    private final Collection<SoftReference<? extends EListener>> registeredListeners = new ConcurrentLinkedQueue<>();

    /**
     * Create a new EventBus. A new single threaded {@link ExecutorService} will be created to service the published events.
     */
    public EventBus() {
        this(Executors.newSingleThreadExecutor());
    }

    /**
     * Create a new EventBus with the given {@link ExecutorService} to handle the asynchronous delivery of the published
     * events.
     *
     * @param executorService The executor that will be used to deliver published events to registered listeners
     */
    public EventBus(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Register a listener object, specifically {@link EventListener} marked methods, with this event bus.
     *
     * @param listener The listener to register
     */
    public void register(Object listener) {
        Objects.requireNonNull(listener, "can not register null listener");
        Reflector.of(listener)
                .methods()
                .stream()
                .filter(method -> method.isAnnotationPresent(EventListener.class))
                .peek(method -> {
                    if (!Modifier.isPublic(method.getModifiers())) {
                        throw new IllegalStateException("@EventListener methods must be public: " + Reflector.detailString(method));
                    }
                    if (method.getParameterCount() != 1) {
                        throw new IllegalArgumentException("@EventListener methods must have 1 and only 1 parameter: " + Reflector.detailString(method));
                    }
                })
                .map(method -> new MethodListener(listener, method, method.getParameterTypes()[0]))
                .map(SoftReference::new)
                .forEach(registeredListeners::add);
    }

    /**
     * Register an event consumer with this event bus.
     *
     * @param type     The type of events to consumer
     * @param consumer The consumer of the events
     */
    @SuppressWarnings("unchecked")
    public <T> void register(Class<T> type, Consumer<T> consumer) {
        registeredListeners.add(new SoftReference<>(new ConsumerListener(type, (Consumer<Object>) consumer)));
    }

    /**
     * Unregister a previously registered listener object or {@link Consumer} instance. Messages will no longer be
     * sent to the unregistered listener.
     *
     * @param listener The listener to unregister
     */
    public void unregister(Object listener) {
        Objects.requireNonNull(listener, "can not unregister null listener");
        Iterator<SoftReference<? extends EListener>> iterator = registeredListeners.iterator();
        while (iterator.hasNext()) {
            EListener l = Optional.ofNullable(iterator.next()).map(SoftReference::get).orElse(null);
            if (l == null || l.isFrom(listener)) {
                iterator.remove();
            }
        }
    }

    /**
     * Publish an event. All applicable listeners (those whose target type is compatible with the event) will receive the
     * event.
     *
     * @param event The event to publish
     */
    public void publish(Object event) {
        Objects.requireNonNull(event, "can not publish null message");
        Iterator<SoftReference<? extends EListener>> iterator = registeredListeners.iterator();
        while (iterator.hasNext()) {
            EListener listener = Optional.ofNullable(iterator.next()).map(SoftReference::get).orElse(null);
            if (listener == null) {
                iterator.remove();
            } else if (listener.canAccept(event)) {
                executorService.submit(() -> listener.accept(event));
            }
        }
    }

    private static abstract class EListener implements Consumer<Object> {
        protected final Class type;

        protected EListener(Class type) {
            this.type = type;
        }

        public boolean canAccept(Object o) {
            return type.isInstance(o);
        }

        public abstract boolean isFrom(Object o);
    }

    private static final class MethodListener extends EListener {

        private final Object instance;
        private final Method method;

        protected MethodListener(Object instance, Method method, Class type) {
            super(type);
            this.instance = instance;
            this.method = method;
        }

        @Override
        public boolean isFrom(Object o) {
            return Objects.equals(instance, o);
        }

        @Override
        public void accept(Object o) {
            try {
                method.invoke(instance, o);
            } catch (Exception e) {
                throw new RuntimeException("error calling message listener: " + Reflector.detailString(method), e);
            }
        }
    }

    private static final class ConsumerListener extends EListener {

        private final Consumer<Object> consumer;

        protected ConsumerListener(Class type, Consumer<Object> consumer) {
            super(type);
            this.consumer = consumer;
        }

        @Override
        public boolean isFrom(Object o) {
            return Objects.equals(consumer, o);
        }

        @Override
        public void accept(Object o) {
            consumer.accept(o);
        }
    }
}
