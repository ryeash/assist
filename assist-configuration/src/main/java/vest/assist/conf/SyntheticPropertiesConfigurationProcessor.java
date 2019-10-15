package vest.assist.conf;

import vest.assist.Assist;
import vest.assist.ConfigurationProcessor;
import vest.assist.aop.AspectInvocationHandler;

import java.lang.reflect.Proxy;

public class SyntheticPropertiesConfigurationProcessor implements ConfigurationProcessor {

    @Override
    @SuppressWarnings("unchecked")
    public void process(Object configuration, Assist assist) {
        for (SyntheticProperties synth : configuration.getClass().getAnnotationsByType(SyntheticProperties.class)) {
            SynthesizedPropertiesAspect instance = assist.instance(SynthesizedPropertiesAspect.class);
            AspectInvocationHandler aih = new AspectInvocationHandler(null, instance);
            Object proxy = Proxy.newProxyInstance(loader(), new Class[]{synth.value()}, aih);
            Class target = synth.value();
            if (synth.name().isEmpty()) {
                assist.setSingleton(target, proxy);
            } else {
                assist.setSingleton(target, synth.name(), proxy);
            }
        }
    }

    @Override
    public int priority() {
        return 100000;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static ClassLoader loader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        } else {
            return ClassLoader.getSystemClassLoader();
        }
    }
}
