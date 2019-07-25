package vest.assist.synthetics.app;

import vest.assist.annotations.Configuration;
import vest.assist.annotations.Factory;
import vest.assist.conf.ConfigurationFacade;
import vest.assist.synthetics.SyntheticProperties;

import javax.inject.Singleton;

@Configuration
//@Synthetic(target = AppProperties.class, aspects = SynthesizedPropertiesAspect.class)
@SyntheticProperties(AppProperties.class)
public class SyntheticsAppConfig {

    @Factory
    @Singleton
    public ConfigurationFacade configurationFacade() {
        return ConfigurationFacade.build()
                .environment()
                .system()
                .structured("./synthetic.properties")
                .enableCaching()
                .enableEnvironments()
                .enableInterpolation()
                .finish();
    }

}
