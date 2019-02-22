package vest.assist.provider;

import vest.assist.Assist;
import vest.assist.AssistProvider;

public class InjectionProvider<T> extends AssistProviderWrapper<T> {

    private final Assist assist;

    public InjectionProvider(AssistProvider<T> delegate, Assist assist) {
        super(delegate);
        this.assist = assist;
    }

    @Override
    public T get() {
        return assist.inject(super.get());
    }
}
