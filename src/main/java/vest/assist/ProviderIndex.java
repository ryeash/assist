package vest.assist;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Stream;

class ProviderIndex {

    private final Node root = new Node();
    private final Map<Class, List<Node>> inverse = new HashMap<>(128);
    private final Map<Class<? extends Annotation>, List<AssistProvider>> annotationTypeToProvider = new HashMap<>(128);
    private final Lock writeLock = new ReentrantLock();
    private int size = 0;

    ProviderIndex() {
    }

    public void setProvider(AssistProvider<?> provider) {
        writeLock.lock();
        try {
            Node temp = root;
            for (Class type : Reflector.of(provider.type()).hierarchy()) {
                temp = temp.getOrCreate(type);
                inverse.computeIfAbsent(type, v -> new ArrayList<>(3)).add(temp);
            }
            temp.putProvider(provider);

            for (Annotation annotation : provider.annotations()) {
                annotationTypeToProvider.computeIfAbsent(annotation.annotationType(), a -> new LinkedList<>()).add(provider);
            }
            size++;
        } finally {
            writeLock.unlock();
        }
    }

    public Provider getProvider(Class type, Annotation qualifier) {
        return Optional.ofNullable(inverse.get(type))
                .orElse(Collections.emptyList())
                .stream()
                .map(n -> n.getProvider(qualifier))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public Provider getOrCreate(Class type, Annotation qualifier, BiFunction<Class, Annotation, AssistProvider<?>> function) {
        Provider provider = getProvider(type, qualifier);
        if (provider != null) {
            return provider;
        }
        writeLock.lock();
        try {
            provider = getProvider(type, qualifier);
            if (provider == null) {
                AssistProvider ap = function.apply(type, qualifier);
                setProvider(ap);
                return ap;
            }
            return provider;
        } finally {
            writeLock.unlock();
        }
    }

    public Stream<AssistProvider> getProviders(Class type) {
        return Optional.ofNullable(inverse.get(type))
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(Node::getProviders)
                .distinct();
    }

    public Stream<AssistProvider> getProvidersWithAnnotation(Class<? extends Annotation> type) {
        return annotationTypeToProvider.getOrDefault(type, Collections.emptyList()).stream();
    }

    public boolean exists(Class type, Annotation qualifier) {
        return getProvider(type, qualifier) != null;
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
