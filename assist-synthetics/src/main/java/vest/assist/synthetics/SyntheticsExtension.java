package vest.assist.synthetics;

import vest.assist.Assist;
import vest.assist.AssistExtension;
import vest.assist.ConfigurationProcessor;
import vest.assist.aop.Aspect;
import vest.assist.aop.AspectInvocationHandler;

import java.lang.reflect.Proxy;
import java.util.stream.Stream;

public class SyntheticsExtension implements AssistExtension, ConfigurationProcessor {

    @Override
    public void load(Assist assist) {
        assist.register(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Object configuration, Assist assist) {
        for (Synthetic synthetics : configuration.getClass().getAnnotationsByType(Synthetic.class)) {
            Aspect[] aspects = Stream.of(synthetics.aspects()).map(assist::instance).toArray(Aspect[]::new);
            AspectInvocationHandler aih = new AspectInvocationHandler(null, aspects);
            Object proxy = Proxy.newProxyInstance(loader(), new Class[]{synthetics.target()}, aih);
            for (Aspect aspect : aspects) {
                aspect.init(proxy);
            }
            Class target = synthetics.target();
            if (synthetics.name().isEmpty()) {
                assist.setSingleton(target, proxy);
            } else {
                assist.setSingleton(target, synthetics.name(), proxy);
            }
        }

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

    private static ClassLoader loader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        } else {
            return ClassLoader.getSystemClassLoader();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
