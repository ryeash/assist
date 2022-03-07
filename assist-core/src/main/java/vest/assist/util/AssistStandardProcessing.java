package vest.assist.util;

import vest.assist.Assist;
import vest.assist.AssistContextBootstrapper;
import vest.assist.provider.AspectWrapper;
import vest.assist.provider.InjectAnnotationInterceptor;
import vest.assist.provider.PropertyInjector;
import vest.assist.provider.ProviderTypeValueLookup;
import vest.assist.provider.ScheduledTaskInterceptor;
import vest.assist.provider.ScopeWrapper;
import vest.assist.provider.SingletonScopeFactory;
import vest.assist.provider.ThreadLocalScopeFactory;

public class AssistStandardProcessing implements AssistContextBootstrapper {
    @Override
    public void load(Assist assist) {
        ScopeWrapper scopeWrapper = new ScopeWrapper();
        scopeWrapper.register(new SingletonScopeFactory());
        scopeWrapper.register(new ThreadLocalScopeFactory());
        assist.setSingleton(ScopeWrapper.class, scopeWrapper);
        assist.register(scopeWrapper);
        assist.register(new AspectWrapper(assist));
        assist.register(new ProviderTypeValueLookup(assist));
        assist.register(new PropertyInjector(assist));
        assist.register(new InjectAnnotationInterceptor(assist));
        assist.register(new ScheduledTaskInterceptor(assist));
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
