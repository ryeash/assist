package vest.assist.app;

import vest.assist.ScopeFactory;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

public class RequestScopeFactory implements ScopeFactory<PerRequest> {
    @Override
    public Class<PerRequest> target() {
        return PerRequest.class;
    }

    @Override
    public <T> Provider<T> scope(Provider<T> provider, Annotation scope) {
        return null;
    }

    public static final class RequestScopeProvider {

    }
}
