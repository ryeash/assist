package vest.assist;

import vest.assist.provider.PrimaryProvider;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    private final Map<Class<? extends Annotation>, Collection<AssistProvider>> annotationTypeToProvider = new HashMap<>(128);
    private final Lock writeLock = new ReentrantLock();
    private int size = 0;

    ProviderIndex() {
    }

    void setProvider(AssistProvider<?> provider) {
        writeLock.lock();
        try {
            Node temp = root;
            for (Class type : Reflector.of(provider.type()).hierarchy()) {
                temp = temp.getOrCreate(type);
                inverse.computeIfAbsent(type, v -> new ArrayList<>(3)).add(temp);
            }
            temp.putProvider(provider);

            for (Annotation annotation : provider.annotations()) {
                annotationTypeToProvider.computeIfAbsent(annotation.annotationType(), a -> new HashSet<>(8)).add(provider);
            }
            size++;
        } finally {
            writeLock.unlock();
        }
    }

    AssistProvider getProvider(Class type, Annotation qualifier) {
        return Optional.ofNullable(inverse.get(type))
                .orElse(Collections.emptyList())
                .stream()
                .map(n -> n.getProvider(qualifier))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    AssistProvider getOrCreate(Class type, Annotation qualifier, BiFunction<Class, Annotation, AssistProvider<?>> function) {
        AssistProvider provider = getProvider(type, qualifier);
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

    Stream<AssistProvider> getProviders(Class type) {
        return Optional.ofNullable(inverse.get(type))
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(Node::getProviders)
                .distinct();
    }

    Stream<AssistProvider> getProvidersWithAnnotation(Class<? extends Annotation> type) {
        return annotationTypeToProvider.getOrDefault(type, Collections.emptyList()).stream();
    }

    boolean exists(Class type, Annotation qualifier) {
        return getProvider(type, qualifier) != null;
    }

    Stream<AssistProvider> allProviders() {
        return root.getProviders();
    }

    int size() {
        return size;
    }

    private static final class Node {
        private Map<Class, Node> sub;
        private Map<Annotation, AssistProvider> providers;

        @SuppressWarnings("unchecked")
        void putProvider(AssistProvider provider) {
            if (providers == null) {
                providers = new HashMap<>(16);
            }
            if (providers.containsKey(provider.qualifier())) {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.type());
            }
            providers.put(provider.qualifier(), provider);

            if (provider.qualifier() != null && provider.primary()) {
                if (providers.containsKey(null)) {
                    throw new IllegalArgumentException("there is already a primary provider registered for type: " + provider.type());
                }
                providers.putIfAbsent(null, new PrimaryProvider<>(provider));
            }
        }

        Node getOrCreate(Class type) {
            if (sub == null) {
                sub = new HashMap<>(16);
            }
            return sub.computeIfAbsent(type, t -> new Node());
        }

        AssistProvider getProvider(Annotation qualifier) {
            if (providers != null && providers.containsKey(qualifier)) {
                AssistProvider provider = providers.get(qualifier);
                if (provider != null) {
                    return provider;
                }
            }
            if (sub != null) {
                for (Node value : sub.values()) {
                    AssistProvider provider = value.getProvider(qualifier);
                    if (provider != null) {
                        return provider;
                    }
                }
            }
            return null;
        }

        Stream<AssistProvider> getProviders() {
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
