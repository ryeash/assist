package vest.assist.conf;

import vest.assist.Assist;
import vest.assist.AssistExtension;
import vest.assist.provider.PropertyInjector;

public class ConfigurationExtension implements AssistExtension {
    @Override
    public void load(Assist assist) {
        assist.register(new PropertyInjector(assist));
        assist.register(new SyntheticPropertiesConfigurationProcessor());
    }
}
