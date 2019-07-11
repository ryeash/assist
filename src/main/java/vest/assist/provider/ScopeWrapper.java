package vest.assist.provider;

import vest.assist.AssistProvider;
import vest.assist.ProviderWrapper;
import vest.assist.ScopeFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Used internally to attach a scope to a provider.
 */
public class ScopeWrapper implements ProviderWrapper {
    private final Map<Class<? extends Annotation>, ScopeFactory<?>> scopeFactories = new HashMap<>(8);

    @Override
    public <T> AssistProvider<T> wrap(AssistProvider<T> provider) {
        Annotation scope = provider.scope();
        if (scope == null) {
            return provider;
        }
        ScopeFactory<?> scopeFactory = Objects.requireNonNull(scopeFactories.get(scope.annotationType()), "unknown scope: " + scope + ", register a scope factory to define scope wrappers");
        return scopeFactory.scope(provider, scope);
    }

    public boolean register(ScopeFactory<?> scopeFactory) {
        if (!scopeFactories.containsKey(scopeFactory.target())) {
            scopeFactories.put(scopeFactory.target(), scopeFactory);
            return true;
        }
        return false;
    }

    public Set<Map.Entry<Class<? extends Annotation>, ScopeFactory<?>>> entrySet() {
        return scopeFactories.entrySet();
    }

    @Override
    public int priority() {
        return 100000;
    }

    @Override
    public String toString() {
        return "ScopeWrapper{scopeFactories=" + scopeFactories + '}';
    }
}
