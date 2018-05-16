package vest.assist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.assist.annotations.Factory;
import vest.assist.annotations.Scan;
import vest.assist.annotations.ThreadLocal;
import vest.assist.provider.ScheduledTaskInterceptor;
import vest.assist.provider.AdHocProvider;
import vest.assist.provider.ConstructorProvider;
import vest.assist.provider.FactoryMethodProvider;
import vest.assist.provider.InjectAnnotationInterceptor;
import vest.assist.provider.ShutdownContainer;
import vest.assist.provider.SingletonScopeProvider;
import vest.assist.provider.ThreadLocalScopeProvider;

import javax.inject.Provider;
import javax.inject.Scope;
import javax.inject.Singleton;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The primary class of the assist library. Provides dependency injection based on javax.inject.
 * <br/>
 * Additionally, has a main method that can be used to quickly bootstrap and start (in conjunction with an
 * application configuration class) an application.
 * Example: <code>java -cp ... vest.assist.Assist your.app.Config [additional parameters]</code>
 */
public class Assist implements Closeable {

    private static Logger log = LoggerFactory.getLogger(Assist.class);

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
            Runtime.getRuntime().addShutdownHook(new Thread(assist::close, "assist-shutdown"));
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private final Map<ClassQualifier, List<Provider>> map = new ConcurrentHashMap<>(64, .9F, 2);
    private final Map<Class<? extends Annotation>, Class<? extends ScopeProvider>> scopeProviders = new HashMap<>();
    private final List<ValueLookup> valueLookups = new LinkedList<>();
    private final List<InstanceInterceptor> interceptors = new LinkedList<>();
    private final ShutdownContainer shutdownContainer;

    /**
     * Create a new Assist instance.
     */
    public Assist() {
        addScopeProvider(Singleton.class, SingletonScopeProvider.class);
        addScopeProvider(ThreadLocal.class, ThreadLocalScopeProvider.class);
        addValueLookup(new ProviderTypeValueLookup(this));

        // allow the Assist to inject itself into object instances
        setSingleton(Assist.class, this);

        addInstanceInterceptor(new InjectAnnotationInterceptor(this));
        addInstanceInterceptor(new ScheduledTaskInterceptor(this));

        this.shutdownContainer = new ShutdownContainer();
        addInstanceInterceptor(this.shutdownContainer);
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
            Class<?> type = loadClass(canonicalClassName);
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
    public <T> Provider<T> providerFor(Class<T> type, Annotation qualifier) {
        Objects.requireNonNull(type);
        return getProvider(new ClassQualifier(type, qualifier), () -> {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                throw new RuntimeException("no provider for " + type + "/" + qualifier + " found, and can not auto-create interfaces/abstract classes");
            }
            if (qualifier != null) {
                throw new RuntimeException("no provider for " + type + "/" + qualifier + " found, and can not auto-create qualified provider");
            }
            return buildProvider(type);
        });
    }

    /**
     * Get a stream of all providers that can provide the given type.
     *
     * @param type The type that the providers will provide
     * @return A Stream of all Providers that can provide the given type
     */
    @SuppressWarnings("unchecked")
    public <T> Stream<Provider<T>> providersFor(Class<T> type) {
        return map.entrySet().stream()
                .filter(e -> type.isAssignableFrom(e.getKey().type()))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .map(p -> (Provider<T>) p);
    }

    /**
     * Get a steram of all providers than can provide the given type.
     *
     * @param type      The type that the provider will provider
     * @param qualifier The qualifier to select using
     * @return A Stream of all Providers that can provide the given typ
     */
    @SuppressWarnings("unchecked")
    public <T> Stream<Provider<T>> providersFor(Class<T> type, Annotation qualifier) {
        return map.entrySet().stream()
                .filter(e -> type.isAssignableFrom(e.getKey().type()) && Objects.equals(qualifier, e.getKey().qualifier))
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
            Class<?> configClass = loadClass(configClassName);
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
                Class<?> returnType = method.getReturnType();
                if (returnType == Void.TYPE) {
                    throw new IllegalArgumentException(config.getClass().getSimpleName() + ": factory methods may not return void: " + method);
                }

                FactoryMethodProvider factory = new FactoryMethodProvider(method, config, this);

                log.info("{}: adding provider {} {}", config.getClass().getSimpleName(), returnType.getSimpleName(), factory);
                setProvider(method.getReturnType(), factory.qualifier(), factory);
                if (factory.qualifier() != null && factory.isPrimary()) {
                    log.info("\\- will be added as primary provider");
                    setProvider(method.getReturnType(), null, factory);
                }
                if (factory.isEager()) {
                    eagerFactories.add(factory);
                }
            }
        }

        // handle the @Scan annotation on the configuration class which will trigger a package scan to create
        // instances of objects with matching target annotation.
        for (Scan scan : config.getClass().getAnnotationsByType(Scan.class)) {
            packageScan(scan.value(), scan.target());
        }

        // auto config for ValueLookups and InstanceInterceptors configured via factory methods
        providersFor(ValueLookup.class).map(Provider::get).forEach(this::addValueLookup);
        providersFor(InstanceInterceptor.class).map(Provider::get).forEach(this::addInstanceInterceptor);

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
     * Register a ScopeProvider for the given scope annotation. When a Provider annotated with a registered scope
     * is processed, an instance of the given ScopeProvider will be created to apply the scope to the Provider.
     *
     * @param scope               The scope annotation. The annotation must have the @Scope annotation on it or a RuntimeException is thrown.
     * @param scopedProviderClass The concrete implementation class that will be tied to the given scope
     */
    public void addScopeProvider(Class<? extends Annotation> scope, Class<? extends ScopeProvider> scopedProviderClass) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(scopedProviderClass);
        // validate that the scope annotation does indeed have a @Scope annotation
        if (!scope.isAnnotationPresent(Scope.class)) {
            throw new IllegalArgumentException("annotation class " + scope.getCanonicalName() + " is not a @Scope annotation");
        }
        if (scopeProviders.containsKey(scope)) {
            throw new IllegalArgumentException("scope already registered: " + scope);
        }
        scopeProviders.put(scope, scopedProviderClass);
    }

    /**
     * Create an instance of a ScopeProvider based on the given scope annotation.
     *
     * @param scope The scope instance to create a provider for
     * @return A ScopeProvider instance (wired by this Assist)
     * @throws IllegalArgumentException if there is no ScopeProvider class registered for the given scope
     */
    @SuppressWarnings("unchecked")
    public <T> ScopeProvider<T> createScopeProvider(Annotation scope) {
        if (scope == null) {
            return null;
        } else if (scopeProviders.containsKey(scope.annotationType())) {
            return instance(scopeProviders.get(scope.annotationType()));
        } else {
            throw new IllegalArgumentException("unknown scope: " + scope + ", use addScopeProvider(...) to define scope providers");
        }
    }

    /**
     * Add an additional {@link ValueLookup} to the list. ValueLookups are used to find values for injectable targets;
     * e.g. fields and parameters.
     *
     * @param valueLookup The value lookup to add
     */
    public void addValueLookup(ValueLookup valueLookup) {
        Objects.requireNonNull(valueLookup);
        if (valueLookups.contains(valueLookup)) {
            throw new IllegalArgumentException("value lookup is already registered: " + valueLookup);
        }
        valueLookups.add(valueLookup);
        valueLookups.sort(Prioritized.PRIORITIZED_COMPARATOR);
    }

    /**
     * Add an additional {@link InstanceInterceptor} to the intercept processing chain. InstanceInterceptors are used
     * to inject field values and call methods after an object has been instantiated.
     *
     * @param interceptor The interceptor to add
     */
    public void addInstanceInterceptor(InstanceInterceptor interceptor) {
        Objects.requireNonNull(interceptor);
        if (interceptors.contains(interceptor)) {
            throw new IllegalArgumentException("interceptor is already registered: " + interceptor);
        }
        interceptors.add(interceptor);
        interceptors.sort(Prioritized.PRIORITIZED_COMPARATOR);
    }

    /**
     * Set the concrete class that implements or extends the given interface or abstract class. This can be used as a
     * shorthand in place of using an application configuration class in instances where the app architecture is simple
     * enough that predefining a few implementations is all that is really needed. Functionally, this method is
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
     *                               Must be either an interface or an abstract class, or a RuntimeException is thrown.
     * @param concreteImplementation The concrete implementation of the interface or abstract class.
     *                               Must not be an interface or abstract class, or a RuntimeException is thrown.
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
        setProvider(interfaceOrAbstract, ref.qualifier(), new ConstructorProvider<>(interfaceOrAbstract, concreteImplementation, this));
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
    public <T> void setProvider(Class<T> type, Annotation qualifier, Provider<T> provider) {
        synchronized (map) {
            // check if there is already a matching provider registered
            List<Provider> Providers = map.get(new ClassQualifier(type, qualifier));
            if (Providers != null) {
                // there is already an exact matching provider for the type/qualifier combination, can't add this one
                throw new IllegalArgumentException("provider for [" + type + "/" + qualifier + "] already exists");
            }
            // register this provider under all classes in its hierarchy
            for (Class<?> superType : Reflector.of(type).hierarchy()) {
                List<Provider> list = map.computeIfAbsent(new ClassQualifier(superType, qualifier), t -> new LinkedList<>());
                list.add(provider);
            }
        }
    }

    /**
     * Scan the classpath (recursively) for classes with the given base package and target annotation and create
     * instances of them. Instances will be injected normally.
     *
     * @param basePackage The base package to start the scan in;
     *                    example: com.foo.service
     * @param target      The annotation that will be used to select which classes to create instances for;
     *                    example: Singleton.class
     */
    @SuppressWarnings("unchecked")
    public void packageScan(String basePackage, Class<? extends Annotation> target) {
        log.info("scanning classpath under {} for @{} classes", basePackage, target.getSimpleName());
        PackageScanner.scan(basePackage)
                .filter(c -> c.isAnnotationPresent(target))
                .peek(c -> log.info("  scanned class: {}", c))
                .forEach(type -> {
                    Annotation qualifier = Reflector.of(type).qualifier();
                    getProvider(new ClassQualifier(type, qualifier), () -> buildProvider(type));
                });
    }

    /**
     * Build a qualified provider based on the injectable constructor for the type.
     *
     * @param type The type to build the provider for
     * @return A new Provider that will provide scoped instances of the given type
     */
    public <T> Provider<T> buildProvider(Class<T> type) {
        return new ConstructorProvider<>(type, this);
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
        // give the registered ValueLookups the first chance to find a value
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Scopes:\n")
                .append(scopeProviders.entrySet().stream()
                        .map(e -> '@' + e.getKey().getSimpleName() + ':' + e.getValue().getSimpleName())
                        .collect(Collectors.joining(", ", "  [", "]\n")));

        sb.append("Interceptors:\n  ")
                .append(interceptors.stream().map(i -> i.getClass().getSimpleName() + ":" + i.priority()).collect(Collectors.joining(", ")))
                .append("\n");

        if (!valueLookups.isEmpty()) {
            sb.append(ValueLookup.class.getSimpleName())
                    .append(":\n  ")
                    .append(valueLookups.stream().map(i -> i.toString() + ':' + i.priority()).collect(Collectors.joining(", ")))
                    .append("\n");
        }

        sb.append("Providers(").append(map.size()).append("):");
        map.keySet().stream()
                .sorted(Comparator.comparing(cq -> cq.type().getSimpleName()))
                .collect(Collectors.groupingBy(ClassQualifier::type, LinkedHashMap::new, Collectors.toSet()))
                .forEach((type, c) -> {
                    sb.append("\n  ").append(type.getSimpleName());
                    c.stream()
                            .flatMap(cq -> map.get(cq).stream().map(v -> toString(cq, v)))
                            .sorted()
                            .forEach(s -> sb.append("\n    ").append(s));
                });

        return sb.toString();
    }

    private static String toString(ClassQualifier classQualifier, Provider provider) {
        return (classQualifier.qualifier() != null ? classQualifier.qualifier() + "/" : "") + String.valueOf(provider);
    }

    @Override
    public void close() {
        shutdownContainer.close();
    }

    @SuppressWarnings("unchecked")
    private <T> Provider<T> getProvider(ClassQualifier classQualifier, Supplier<Provider<T>> ifMissing) {
        List<Provider> Providers = map.get(classQualifier);
        if (Providers != null && !Providers.isEmpty()) {
            return Providers.get(0);
        }

        if (ifMissing == null) {
            return null;
        }

        synchronized (map) {
            Providers = map.get(classQualifier);
            if (Providers != null && !Providers.isEmpty()) {
                return Providers.get(0);
            }

            Provider<?> created = ifMissing.get();
            setProvider(classQualifier.type(), classQualifier.qualifier(), created);
            return (Provider<T>) created;
        }
    }

    /**
     * Load a class name using the Thread context class loader if possible, else use {@link Class#forName(String)}.
     *
     * @param canonicalClassName The full name of the class to load.
     * @return The loaded class
     * @throws ClassNotFoundException if the class does not exist in the class loader
     */
    public static Class<?> loadClass(String canonicalClassName) throws ClassNotFoundException {
        if (Thread.currentThread().getContextClassLoader() != null) {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(canonicalClassName);
            } catch (ClassNotFoundException c) {
                // ignored
            }
        }
        return Class.forName(canonicalClassName);
    }
}
