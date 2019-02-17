package vest.assist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.annotations.Aspects;
import vest.assist.annotations.Factory;
import vest.assist.annotations.Scan;
import vest.assist.provider.AdHocProvider;
import vest.assist.provider.AspectWeaverProvider;
import vest.assist.provider.ConstructorProvider;
import vest.assist.provider.FactoryMethodProvider;
import vest.assist.provider.InjectAnnotationInterceptor;
import vest.assist.provider.InjectionProvider;
import vest.assist.provider.LazyProvider;
import vest.assist.provider.PropertyInjector;
import vest.assist.provider.ScheduledTaskInterceptor;
import vest.assist.provider.ShutdownContainer;
import vest.assist.provider.SingletonScopeFactory;
import vest.assist.provider.ThreadLocalScopeFactory;

import javax.inject.Provider;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The primary class of the assist library. Provides dependency injection based on javax.inject.
 * <br/>
 * Additionally, has a main method that can be used to quickly bootstrap and start (in conjunction with an
 * application configuration class) an application.
 */
public class Assist implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(Assist.class);

    /**
     * Usable as the entry point into an application. Requires a fully qualified class name be given as the first command
     * line argument in order to initiate class wiring and injection.
     * <br>
     * Example:
     * <br><code>java -cp ... vest.assist.Assist your.app.Config [additional parameters]</code>
     * <br>Or if you create a shaded jar with Assist as the main class:
     * <br><code>java -jar your-uber.jar your.app.Config [additional parameters]</code>
     *
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("error: must provide the fully qualified application configuration class as the first argument");
            System.out.println("ex: java -cp ... vest.assist.Assist my.org.AppConfig [additional params]");
            System.exit(1);
        }
        Assist assist = new Assist();
        try {
            Args a = new Args(args);
            assist.setSingleton(Args.class, a);
            assist.addConfig(a.first());
            assist.autoShutdown();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private final Map<ClassQualifier, List<AssistProvider>> map = new HashMap<>(128);
    private final Map<Class<? extends Annotation>, ScopeFactory<?>> scopeFactories = new HashMap<>(8);
    private final List<ValueLookup> valueLookups = new ArrayList<>(8);
    private final List<InstanceInterceptor> interceptors = new ArrayList<>(8);
    private final ShutdownContainer shutdownContainer;

    private final java.lang.ThreadLocal<Map<ClassQualifier, Provider>> threadLocalOverrides = new java.lang.ThreadLocal<>();

    private final Lock setLock = new ReentrantLock();
    private final Lock createLock = new ReentrantLock();

    /**
     * Create a new Assist instance.
     */
    public Assist() {
        register(new SingletonScopeFactory());
        register(new ThreadLocalScopeFactory());
        register(new ProviderTypeValueLookup(this));
        register(new PropertyInjector(this));
        register(new InjectAnnotationInterceptor(this));
        register(new ScheduledTaskInterceptor(this));
        this.shutdownContainer = new ShutdownContainer();
        register(this.shutdownContainer);

        // allow the Assist to inject itself into object instances
        setSingleton(Assist.class, this);

    }

    /**
     * Get an instance of a type specified by the fully qualified class name of the type.
     *
     * @param canonicalClassName The fully qualified class name of the type to get an instance of;
     *                           e.g. "com.my.package.MyComponent"
     * @return An instance of the type
     * @throws IllegalArgumentException is no class exists for the given name
     * @see Assist#instance(Class)
     */
    @SuppressWarnings("unchecked")
    public <T> T instance(String canonicalClassName) {
        try {
            Class<?> type = Reflector.loadClass(canonicalClassName);
            return (T) instance(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("unknown class " + canonicalClassName, e);
        }
    }

    /**
     * Get an instance of the given type. If there is a Provider already configured that can return a class that
     * satisfies the given type, that provider is used to get the object instance, if no Provider exists, an attempt
     * will be made to create one based on the injectable constructor of the given type. If no Provider exists, and it
     * is impossible to automatically create one, a RuntimeException is thrown.
     *
     * @param type The object type to get an instance of
     * @return An instance of the object type
     */
    public <T> T instance(Class<T> type) {
        return providerFor(type).get();
    }

    /**
     * Get a named instance of the given type; see {@link Assist#providerFor(Class, String)}.
     *
     * @param type The object type to get an instance of
     * @param name The qualifier name of the provider to use to get the instance
     * @return An instance of the named object type
     */
    public <T> T instance(Class<T> type, String name) {
        return providerFor(type, name).get();
    }

    /**
     * Get a qualified instance of the given type; see {@link Assist#providerFor(Class, Annotation)}.
     *
     * @param type      The object type to get an instance of
     * @param qualifier The qualifier of the of the provider to use to get the instance
     * @return An instance of the qualifier object type
     */
    public <T> T instance(Class<T> type, Annotation qualifier) {
        return providerFor(type, qualifier).get();
    }

    /**
     * Get all instances of the given type.
     *
     * @param type The object type to get the instances for
     * @return A list of all instances provided by the registered Providers that are type compatible with the requested type
     */
    public <T> Stream<T> instances(Class<T> type) {
        return providersFor(type).map(Provider::get);
    }

    /**
     * Get a provider that can supply a class that satisfies the given type. If there is a Provider already configured
     * that can return a class that satisfies the given type, that provider is returned, if no Provider exists, an
     * attempt will be made to create one based on the injectable constructor of the given type. If no Provider exists,
     * and it is impossible to automatically create one, a RuntimeException is thrown.
     *
     * @param type The type of object the provider will provide
     * @return A Provider that can supply a class that satisfies the given type
     */
    public <T> Provider<T> providerFor(Class<T> type) {
        return providerFor(type, (Annotation) null);
    }

    /**
     * Get a named Provider that can supply a class that satisfies the given type. If there is a Provider already configured
     * that can return a class that satisfies the given type, that provider is returned, if no such Provider exists, a
     * RuntimeException is thrown.
     *
     * @param type The type of object the provider will provide
     * @param name The qualifier name of the provider.
     * @return The named Provider that can supply a class that satisfies the given type
     */
    public <T> Provider<T> providerFor(Class<T> type, String name) {
        return providerFor(type, new NamedImpl(Objects.requireNonNull(name, "name may not be null")));
    }

    /**
     * Get a provider that can supply a class that satisfies the given type and qualifier.
     * If there is a Provider already configured that can return a class that satisfies the given type, that provider
     * is returned, if no Provider exists, and the qualifier is null an attempt will be made to create one based on
     * the injectable constructor of the given type. If no Provider exists, and it is impossible to automatically
     * create one, a RuntimeException is thrown.
     *
     * @param type      The type of object the provider will return
     * @param qualifier The qualifier of the expected class instance, used to delineate between different
     *                  implementations of the same interface
     * @return A Provider that can supply a class that satisfies the given type and qualifier
     */
    @SuppressWarnings("unchecked")
    public <T> Provider<T> providerFor(Class<T> type, Annotation qualifier) {
        Objects.requireNonNull(type);
        ClassQualifier classQualifier = new ClassQualifier(type, qualifier);
        return getProvider(classQualifier, (t, q) -> {
            if (t.isInterface() || Modifier.isAbstract(t.getModifiers())) {
                throw new RuntimeException("no provider for " + t + "/" + q + " found, and can not auto-create interfaces/abstract classes");
            }
            if (q != null) {
                throw new RuntimeException("no provider for " + t + "/" + q + " found, and can not auto-create qualified provider");
            }
            return buildProvider(t);
        });
    }

    /**
     * Build a lazy provider that can supply a class that satisfies the given type and qualifier. On first call to
     * {@link Provider#get}, if no provider exists that can satisfy the type/qualifier combination, the method will
     * throw an exception.
     *
     * @param type      the type the provider will return
     * @param qualifier the qualifier for the type
     * @return a lazy provider that will provide the desired type/qualifier combination
     */
    public <T> Provider<T> lazyProviderFor(Class<T> type, Annotation qualifier) {
        return new LazyProvider<>(this, type, qualifier);
    }

    /**
     * Get a stream of all providers that can provide the given type.
     *
     * @param type The type that the providers will provide
     * @return A Stream of all Providers that can provide the given type
     */
    @SuppressWarnings("unchecked")
    public <T> Stream<Provider<T>> providersFor(Class<T> type) {
        return map.entrySet()
                .stream()
                .filter(e -> type.isAssignableFrom(e.getKey().type()))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .map(p -> (Provider<T>) p);
    }

    /**
     * Get a stream of all providers than can provide the given type.
     *
     * @param type      The type that the provider will provider
     * @param qualifier The qualifier to select using
     * @return A Stream of all Providers that can provide the given typ
     */
    @SuppressWarnings("unchecked")
    public <T> Stream<Provider<T>> providersFor(Class<T> type, Annotation qualifier) {
        return map.entrySet().stream()
                .filter(e -> type.isAssignableFrom(e.getKey().type()) && Objects.equals(qualifier, e.getKey().qualifier()))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .map(p -> (Provider<T>) p);
    }

    /**
     * Given an instance of an object, run the post-instantiation injection steps defined in this Assist instance
     * against it.
     *
     * @param instance The object instance to inject
     * @return The given instance
     */
    public <T> T inject(T instance) {
        Objects.requireNonNull(instance, "null pointers can not be injected");
        for (InstanceInterceptor interceptor : interceptors) {
            interceptor.intercept(instance);
        }
        return instance;
    }

    /**
     * Get the class (using Class.forName()), and passes it to {@link #addConfig(Class configClass)}
     *
     * @param configClassName The string name of the configuration class, must be fully qualified
     * @see #addConfig(Class configClass)
     */
    public void addConfig(String configClassName) {
        try {
            Class<?> configClass = Reflector.loadClass(configClassName);
            addConfig(configClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("configuration class does not exist: " + configClassName, e);
        }
    }

    /**
     * Attempts to instantiate the given class and passes it to the addConfig(Object) method. The injectable constructor
     * will be used to instantiate the object; if no injectable constructor exists, a RuntimeException will be thrown.
     *
     * @param configClass The configuration class to instantiate
     * @throws RuntimeException if the given class is not injectable
     * @see #addConfig(Object configuration)
     */
    public void addConfig(Class<?> configClass) {
        // don't just use the instance(...) method because we don't want the config object available through a Provider
        addConfig(buildProvider(configClass).get());
    }

    /**
     * Add a configuration object to this Assist instance. Configuration objects define @Factory methods that are
     * analyzed and turned into {@link Provider}s.
     *
     * @param config The configuration object
     */
    @SuppressWarnings("unchecked")
    public void addConfig(Object config) {
        Objects.requireNonNull(config, "can not process null configuration class");
        log.info("processing configuration class: {}", config.getClass().getCanonicalName());
        // create providers from the @Factory methods
        Reflector reflector = Reflector.of(config);

        List<FactoryMethodProvider> eagerFactories = new LinkedList<>();
        for (Method method : reflector.methods()) {
            if (method.isAnnotationPresent(Factory.class)) {
                Factory factoryAnnotation = method.getAnnotation(Factory.class);
                Class<?> returnType = method.getReturnType();
                if (returnType == Void.TYPE) {
                    throw new IllegalArgumentException(config.getClass().getSimpleName() + ": factory methods may not return void: " + Reflector.detailString(method));
                }

                FactoryMethodProvider factory = new FactoryMethodProvider(method, config, this);
                Annotation scope = Reflector.getScope(method);

                AssistProvider p = factory;
                if (!factoryAnnotation.skipInjection()) {
                    p = wrapInjection(p);
                }
                p = wrapAspects(method, returnType, p);
                p = wrapScope(scope, p);

                log.info("{}: adding provider {} {}", config.getClass().getSimpleName(), returnType.getSimpleName(), p);
                setProvider(method.getReturnType(), factory.qualifier(), p);
                if (factory.qualifier() != null && factory.isPrimary()) {
                    log.info("\\- will be added as primary provider");
                    setProvider(method.getReturnType(), null, p);
                }
                if (factory.isEager()) {
                    eagerFactories.add(factory);
                }
            }
        }

        // handle the @Scan annotation on the configuration class which will trigger a package scan to create
        // instances of objects with matching target annotation.
        for (Scan scan : config.getClass().getAnnotationsByType(Scan.class)) {
            for (String basePackage : scan.value()) {
                packageScan(basePackage, scan.target());
            }
        }

        // handle eager @Factory methods (they must be called after all other processing to avoid missing dependencies).
        for (FactoryMethodProvider eagerFactory : eagerFactories) {
            log.info("{}: calling eager factory provider: {}", config.getClass().getSimpleName(), eagerFactory);
            eagerFactory.get();
        }

        // run through the injection workflow as the last step
        // allows e.g. @Inject methods to be called that include any final initialization to happen
        inject(config);

        log.info("Finished processing configuration class {}", config.getClass().getCanonicalName());
    }

    /**
     * Register an instance of a custom assist component (such as an {@link InstanceInterceptor}) to be used during
     * injection processing in the context of this Assist instance.
     *
     * @param obj the object to register
     * @see InstanceInterceptor
     * @see ValueLookup
     * @see ScopeFactory
     */
    @SuppressWarnings("unchecked")
    public void register(Object obj) {
        boolean registered = false;
        if (obj instanceof ValueLookup) {
            ValueLookup valueLookup = (ValueLookup) obj;
            if (!valueLookups.contains(obj)) {
                registered = true;
                valueLookups.add(valueLookup);
                valueLookups.sort(Prioritized.PRIORITIZED_COMPARATOR);
            }
        }

        if (obj instanceof InstanceInterceptor) {
            InstanceInterceptor interceptor = (InstanceInterceptor) obj;
            if (!interceptors.contains(interceptor)) {
                registered = true;
                interceptors.add(interceptor);
                interceptors.sort(Prioritized.PRIORITIZED_COMPARATOR);
            }
        }

        if (obj instanceof ScopeFactory) {
            ScopeFactory scopeFactory = (ScopeFactory) obj;
            if (!scopeFactories.containsKey(scopeFactory.target())) {
                registered = true;
                scopeFactories.put(scopeFactory.target(), scopeFactory);
            }
        }

        if (!registered) {
            log.warn("{} did not implement any known interfaces", obj);
        }
    }

    private <T> AssistProvider<T> wrapInjection(AssistProvider<T> provider) {
        return new InjectionProvider<>(provider, this);
    }

    private <T> AssistProvider<T> wrapScope(Annotation scope, AssistProvider<T> provider) {
        if (scope == null) {
            return provider;
        }
        ScopeFactory<?> scopeFactory = Objects.requireNonNull(scopeFactories.get(scope.annotationType()), "unknown scope: " + scope + ", register a scope factory to define scope wrappers");
        return scopeFactory.scope(provider, scope);
    }

    private <T> AssistProvider<T> wrapAspects(AnnotatedElement annotatedElement, Class<T> type, AssistProvider<T> provider) {
        Aspects aop = annotatedElement.getAnnotation(Aspects.class);
        if (aop != null) {
            return new AspectWeaverProvider<>(this, aop.value(), type, provider);
        } else {
            return provider;
        }
    }

    /**
     * Set the concrete class that implements or extends the given interface or abstract class. This can be used as a
     * shorthand in place of using an application configuration class in instances where the app architecture is simple
     * enough that predefining a few implementations is all that is needed. Functionally, this method is
     * creating a {@link ConstructorProvider} for the concrete class and registering it under the interface. As a
     * result, errors will be throw if the concrete class has no injectable constructor or if there is
     * already a provider that can provide the interface and qualifier (defined on the concrete class) combination.
     * The scope and qualifier (if present) on the concreteImplementation will be honored, so it is possible to
     * register multiple different concrete implementations if they have different qualifiers.
     * <br><br>
     * <strong>Example:</strong><br>
     * <code>
     * Assist n = new Assist();<br>
     * n.addImplementingClass(CoffeeMaker.class, FrenchPress.class);<br>
     * CoffeeMaker cm = n.instance(CoffeeMaker.class);<br>
     * assert cm.getClass() == FrenchPress.class; // true
     * </code>
     *
     * @param interfaceOrAbstract    The interface or abstract class for which the concrete class will be registered under.
     *                               Must be either an interface or an abstract class else an IllegalArgumentException is thrown.
     * @param concreteImplementation The concrete implementation of the interface or abstract class.
     *                               Must not be an interface or abstract class else an IllegalArgumentException is thrown.
     */
    public <T> void addImplementingClass(Class<T> interfaceOrAbstract, Class<? extends T> concreteImplementation) {
        Objects.requireNonNull(interfaceOrAbstract);
        Objects.requireNonNull(concreteImplementation);
        if (!(interfaceOrAbstract.isInterface() || Modifier.isAbstract(interfaceOrAbstract.getModifiers()))) {
            throw new IllegalArgumentException("first argument must be either an interface or an abstract class");
        }
        if (concreteImplementation.isInterface() || Modifier.isAbstract(concreteImplementation.getModifiers())) {
            throw new IllegalArgumentException("second argument must be a concrete class");
        }
        Reflector ref = Reflector.of(concreteImplementation);
        Annotation scope = ref.scope();
        AssistProvider<T> provider = new ConstructorProvider<>(interfaceOrAbstract, concreteImplementation, this);
        provider = wrapInjection(provider);
        setProvider(interfaceOrAbstract, ref.qualifier(), wrapScope(scope, provider));
    }

    /**
     * Check if there is an unqualified provider for the given type
     *
     * @param type The provider type to check
     * @return True if this Assist instance has an unqualified provider for the type
     */
    public boolean hasProvider(Class<?> type) {
        return hasProvider(type, (Annotation) null);
    }

    /**
     * Check if there is a provider that can satisfy the given class type and qualifier name
     *
     * @param type The provider type to check
     * @param name The qualifier name of the provider
     * @return True if this Assist instance has a provider that satisfies the type and name
     */
    public boolean hasProvider(Class<?> type, String name) {
        Objects.requireNonNull(name);
        return hasProvider(type, new NamedImpl(name));
    }

    /**
     * Check if there is a provider that can satisfy the given class type and qualifier
     *
     * @param type      The provided type to check
     * @param qualifier The qualifier of the provider to check, can be null
     * @return True if this Assist instance has a provider that satisfies the type and qualifier
     */
    public boolean hasProvider(Class<?> type, Annotation qualifier) {
        Objects.requireNonNull(type);
        return getProvider(new ClassQualifier(type, qualifier), null) != null;
    }

    /**
     * Set the Singleton instance of a class. Registers a new Provider for the given type that always returns the given instance.
     *
     * @param type     The type to register the singleton as.
     * @param instance The instance to register
     */
    public <T> void setSingleton(Class<? super T> type, T instance) {
        setProvider(type, null, new AdHocProvider<>(instance));
    }

    /**
     * Set a provider.
     *
     * @param provider The provider
     * @throws IllegalArgumentException if the type/qualifier combination already exists for a Provider
     */
    public <T> void setProvider(Class<T> type, Annotation qualifier, AssistProvider<T> provider) {
        setLock.lock();
        try {
            // check if there is already a matching provider registered
            List<AssistProvider> providers = map.get(new ClassQualifier(type, qualifier));
            if (providers != null) {
                // there is already an exact matching provider for the type/qualifier combination, can't add this one
                throw new IllegalArgumentException("provider for [" + type + "/" + qualifier + "] already exists");
            }
            // register this provider under all classes in its hierarchy
            for (Class<?> superType : Reflector.of(type).hierarchy()) {
                List<AssistProvider> list = map.computeIfAbsent(new ClassQualifier(superType, qualifier), t -> new LinkedList<>());
                list.add(provider);
            }
        } finally {
            setLock.unlock();
        }
    }

    /**
     * Scan the classpath (recursively) for classes with the given base package and target annotation and create
     * instances of them. Instances will be injected normally.
     *
     * @param basePackage The base package to start the scan in, it must not end with a wild card;
     *                    example: com.foo.service
     * @param target      The annotation that will be used to select which classes to create instances for;
     *                    example: Singleton.class
     */
    @SuppressWarnings("unchecked")
    public void packageScan(String basePackage, Class<? extends Annotation> target) {
        packageScan(basePackage, target, type -> {
            log.info("  scanned class: {}", type);
            Annotation qualifier = Reflector.of(type).qualifier();
            Provider<?> provider = getProvider(new ClassQualifier(type, qualifier), (t, q) -> buildProvider(t));
            if (provider != null) {
                provider.get();
            }
        });
    }

    public void packageScan(String basePackage, Class<? extends Annotation> target, Consumer<Class<?>> action) {
        if (basePackage.endsWith("*")) {
            throw new IllegalArgumentException("base package [" + basePackage + "] must not end with '*'");
        }
        log.info("scanning classpath under {} for @{} classes", basePackage, target.getSimpleName());
        PackageScanner.scan(basePackage)
                .filter(c -> c.isAnnotationPresent(target))
                .peek(c -> log.info("  scanned class: {}", c))
                .forEach(action);
    }

    /**
     * Build a qualified provider based on the injectable constructor for the type.
     *
     * @param type The type to build the provider for
     * @return A new Provider that will provide scoped instances of the given type
     */
    public <T> AssistProvider<T> buildProvider(Class<T> type) {
        return wrapScope(Reflector.getScope(type), wrapInjection(new ConstructorProvider<>(type, this)));
    }

    /**
     * Get the injectable input parameter values for the given Executable (method or constructor)
     *
     * @param executable The Executable to get input parameters for (either a Method or Constructor)
     * @return An object array of input parameters for the Executable
     */
    public Object[] getParameterValues(Executable executable) {
        return getParameterValues(executable.getParameters());
    }

    /**
     * Get the parameter values for the given Parameter array.
     *
     * @param parameters The parameters to get values for
     * @return An object array of injectable parameter values
     */
    public Object[] getParameterValues(Parameter[] parameters) {
        Object[] arr = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            arr[i] = valueFor(parameters[i]);
        }
        return arr;
    }

    /**
     * Get the value for a parameter based on its type, annotations, and what is registered/injectable with this Assist instance.
     *
     * @param parameter The parameter to analyze and retrieve a value for
     * @return The value found
     */
    public Object valueFor(Parameter parameter) {
        return valueFor(parameter.getType(), parameter.getParameterizedType(), parameter);
    }

    /**
     * Get the value for a field based on its type, annotations, and what is registered/injectable with this Assist instance.
     *
     * @param field The field to analyze and retrieve a value for
     * @return The value found
     */
    public Object valueFor(Field field) {
        return valueFor(field.getType(), field.getGenericType(), field);
    }

    /**
     * Get the value for an annotated element based on its type, annotations, and what is provided by this Assist instance.
     *
     * @param rawType          The raw type of the annotated element
     * @param genericType      The generic type of the annotated element, may be null
     * @param annotatedElement The annotated element to find an injectable value for
     * @return The injectable value for the the annotated element
     */
    public Object valueFor(Class<?> rawType, Type genericType, AnnotatedElement annotatedElement) {
        try {
            for (ValueLookup valueLookup : valueLookups) {
                Object o = valueLookup.lookup(rawType, genericType, annotatedElement);
                if (o != null) {
                    log.debug("value for {} was found in {}", annotatedElement, valueLookup);
                    return o;
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("error finding injectable value for: " + annotatedElement, e);
        }
        throw new RuntimeException("internal error: no value lookup is configured");
    }

    /**
     * Override the provided instance for a provider using the given instance. Overrides
     * only apply to the current thread. Internally uses a ThreadLocal to track the overridden values.
     *
     * @param instance The instance to use as the overridden provider type
     * @return The provider created for the overridden value
     * @see #override(Class, Annotation, Object)
     */
    @SuppressWarnings("unchecked")
    public <T> Provider<T> override(T instance) {
        return override((Class<T>) instance.getClass(), (Annotation) null, instance);
    }


    /**
     * Override the provided instance for the given type with the given instance. Overrides
     * only apply to the current thread. Internally uses a ThreadLocal to track the overridden values.
     *
     * @param type     The type to override the value for
     * @param instance The instance to use as the overridden provider type
     * @return The provider created for the overridden value
     * @see #override(Class, Annotation, Object)
     */
    public <T> Provider<T> override(Class<T> type, T instance) {
        return override(type, (Annotation) null, instance);
    }

    /**
     * Override the provided instance for the given type and name combination with the given instance. Overrides
     * only apply to the current thread. Internally uses a ThreadLocal to track the overridden values.
     *
     * @param type     The type to override the value for
     * @param name     The name qualifier to use for the type
     * @param instance The instance to use as the overridden provider type
     * @return The provider created for the overridden value
     * @see #override(Class, Annotation, Object)
     */
    public <T> Provider<T> override(Class<T> type, String name, T instance) {
        return override(type, new NamedImpl(name), instance);
    }

    /**
     * Override the provided instance for the given type and qualifier combination with the given instance. Overrides
     * only apply to the current thread. Internally uses a ThreadLocal to track the overridden values.
     *
     * @param type      The type to override the value for
     * @param qualifier The qualifier to use for the type
     * @param instance  The instance to use as the overridden provider type
     * @return The provider created for the overridden value
     */
    public <T> Provider<T> override(Class<T> type, Annotation qualifier, T instance) {
        if (threadLocalOverrides.get() == null) {
            threadLocalOverrides.set(new HashMap<>(4));
        }
        Provider<T> provider = new AdHocProvider<>(instance);
        threadLocalOverrides.get().put(new ClassQualifier(type, qualifier), provider);
        return provider;
    }

    /**
     * Clear all overridden providers.
     */
    public void clearOverrides() {
        threadLocalOverrides.remove();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Scopes:\n  ")
                .append(scopeFactories.entrySet().stream()
                        .map(e -> '@' + e.getKey().getSimpleName() + ':' + e.getValue().getClass().getSimpleName())
                        .collect(Collectors.joining("\n  ")))
                .append("\n");

        sb.append("Interceptors:\n  ")
                .append(interceptors.stream().map(i -> i.toString() + ":" + i.priority()).collect(Collectors.joining("\n  ")))
                .append("\n");

        if (!valueLookups.isEmpty()) {
            sb.append(ValueLookup.class.getSimpleName())
                    .append(":\n  ")
                    .append(valueLookups.stream().map(i -> i.toString() + ':' + i.priority()).collect(Collectors.joining("\n  ")))
                    .append("\n");
        }

        sb.append("Providers(").append(map.size()).append("):");
        map.keySet().stream()
                .sorted(Comparator.comparing(cq -> cq.type().getSimpleName()))
                .collect(Collectors.groupingBy(ClassQualifier::type, LinkedHashMap::new, Collectors.toSet()))
                .forEach((type, c) -> {
                    sb.append("\n  ").append(type.getSimpleName());
                    c.stream()
                            .flatMap(cq -> map.get(cq).stream().map(String::valueOf))
                            .sorted()
                            .forEach(s -> sb.append("\n    ").append(s));
                });

        return sb.toString();
    }

    @Override
    public void close() {
        shutdownContainer.close();
    }

    /**
     * Add a shutdown hook to call {@link #close()} on this Assist instance.
     */
    public void autoShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "assist-shutdown"));
    }

    @SuppressWarnings("unchecked")
    private <T> Provider<T> getProvider(ClassQualifier classQualifier, BiFunction<Class, Annotation, AssistProvider<T>> ifMissing) {
        // check overrides first
        Map<ClassQualifier, Provider> overrideMap = threadLocalOverrides.get();
        if (overrideMap != null) {
            Provider provider = overrideMap.get(classQualifier);
            if (provider != null) {
                return provider;
            }
        }

        // now look in the real provider store
        List<AssistProvider> providers = map.get(classQualifier);
        if (providers != null && !providers.isEmpty()) {
            return providers.get(0);
        }

        if (ifMissing == null) {
            return null;
        }

        createLock.lock();
        try {
            providers = map.get(classQualifier);
            if (providers != null && !providers.isEmpty()) {
                return providers.get(0);
            }

            AssistProvider<T> created = ifMissing.apply(classQualifier.type(), classQualifier.qualifier());
            setProvider(classQualifier.type(), classQualifier.qualifier(), created);
            return created;
        } finally {
            createLock.unlock();
        }
    }

}
