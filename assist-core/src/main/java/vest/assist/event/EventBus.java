package vest.assist.event;

import vest.assist.util.MethodTarget;
import vest.assist.util.Reflector;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple event bus, listeners are registered and receive compatible messages published to the bus.
 * Useful for decoupling code within a JVM.
 */
public class EventBus {
    private final ExecutorService executorService;
    private final Collection<Reference<Listener>> registeredListeners = new ConcurrentLinkedQueue<>();

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
                .map(method -> new Listener(listener, method, method.getParameterTypes()[0]))
                .map(WeakReference::new)
                .forEach(registeredListeners::add);
    }

    /**
     * Unregister a previously registered listener object. Messages will no longer be
     * sent to the unregistered listener.
     *
     * @param listener The listener to unregister
     */
    public void unregister(Object listener) {
        Objects.requireNonNull(listener, "can not unregister null listener");
        Iterator<Reference<Listener>> iterator = registeredListeners.iterator();
        while (iterator.hasNext()) {
            Listener l = Optional.ofNullable(iterator.next()).map(Reference::get).orElse(null);
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
        Iterator<Reference<Listener>> iterator = registeredListeners.iterator();
        while (iterator.hasNext()) {
            Reference<Listener> next = iterator.next();
            if (next == null) {
                iterator.remove();
                continue;
            }
            Listener listener = next.get();
            if (listener == null) {
                iterator.remove();
                continue;
            }
            if (listener.canAccept(event)) {
                executorService.submit(() -> listener.accept(event));
            }
        }
    }

    private static final class Listener {

        private final Class type;
        private final Object instance;
        private final MethodTarget method;

        protected Listener(Object instance, MethodTarget method, Class type) {
            this.type = type;
            this.instance = instance;
            this.method = method;
        }

        public boolean isFrom(Object o) {
            return o == instance;
        }

        public boolean canAccept(Object o) {
            return type.isInstance(o);
        }

        public void accept(Object o) {
            try {
                method.invoke(instance, o);
            } catch (Throwable e) {
                throw new RuntimeException("error calling message listener: " + Reflector.detailString(method), e);
            }
        }
    }
}
