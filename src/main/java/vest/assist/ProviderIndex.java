package vest.assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Stream;

class ProviderIndex {

    private final Node root = new Node();
    private final Map<Class, Node> inverse = new HashMap<>(128);
    private final Map<Class<? extends Annotation>, List<AssistProvider>> annotationTypeToProvider = new HashMap<>(128);
    private final Lock writeLock = new ReentrantLock();
    private final Lock createLock = new ReentrantLock();
    private int size = 0;

    ProviderIndex() {
    }

    public void setProvider(AssistProvider<?> provider) {
        writeLock.lock();
        try {
            Node temp = root;
            for (Class type : Reflector.of(provider.type()).hierarchy()) {
                temp = temp.getOrCreate(type);
                inverse.put(type, temp);
            }
            temp.putProvider(provider);

            for (Annotation annotation : provider.annotations()) {
                annotationTypeToProvider.computeIfAbsent(annotation.annotationType(), a -> new LinkedList<>()).add(provider);
            }
        } finally {
            size++;
            writeLock.unlock();
        }
    }

    public Provider getProvider(Class type, Annotation qualifier) {
        Node node = inverse.get(type);
        if (node != null) {
            Provider provider = node.getProvider(qualifier);
            if (provider != null) {
                return provider;
            }
        }
        return null;
    }

    public Provider getOrCreate(Class type, Annotation qualifier, BiFunction<Class, Annotation, AssistProvider<?>> function) {
        Provider provider = getProvider(type, qualifier);
        if (provider != null) {
            return provider;
        }
        createLock.lock();
        try {
            provider = getProvider(type, qualifier);
            if (provider == null) {
                AssistProvider ap = function.apply(type, qualifier);
                setProvider(ap);
                return ap;
            }
            return provider;
        } finally {
            createLock.unlock();
        }
    }

    public Stream<AssistProvider> getProviders(Class type) {
        Node node = inverse.get(type);
        if (node != null) {
            return node.getProviders();
        } else {
            return Stream.empty();
        }
    }

    public Stream<AssistProvider> getProvidersWithAnnotation(Class<? extends Annotation> type) {
        return annotationTypeToProvider.getOrDefault(type, Collections.emptyList()).stream();
    }

    public boolean exists(Class type, Annotation qualifier) {
        return Optional.ofNullable(inverse.get(type))
                .map(n -> n.getProvider(qualifier))
                .isPresent();
    }

    public Stream<AssistProvider> allProviders() {
        return root.getProviders();
    }

    public int size() {
        return size;
    }

    private static final class Node {
        private Map<Class, Node> sub;
        private Map<Annotation, AssistProvider> providers;

        public void putProvider(AssistProvider provider) {
            if (providers == null) {
                providers = new HashMap<>(16);
            }
            if (providers.containsKey(provider.qualifier())) {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.type());
            }
            providers.put(provider.qualifier(), provider);
        }

        public Node getOrCreate(Class type) {
            if (sub == null) {
                sub = new HashMap<>(16);
            }
            return sub.computeIfAbsent(type, t -> new Node());
        }

        public Provider getProvider(Annotation qualifier) {
            if (providers != null && providers.containsKey(qualifier)) {
                Provider provider = providers.get(qualifier);
                if (provider != null) {
                    return provider;
                }
            }
            if (sub != null) {
                for (Node value : sub.values()) {
                    Provider provider = value.getProvider(qualifier);
                    if (provider != null) {
                        return provider;
                    }
                }
            }
            return null;
        }

        public Stream<AssistProvider> getProviders() {
            Stream<AssistProvider> prim = Stream.empty();
            if (providers != null) {
                prim = providers.values().stream();
            }
            Stream<AssistProvider> desc = Stream.empty();
            if (sub != null) {
                desc = sub.values()
                        .stream()
                        .flatMap(Node::getProviders);
            }
            return Stream.concat(prim, desc);
        }
    }

}
