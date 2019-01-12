[![Build Status](https://travis-ci.org/ryeash/assist.svg?branch=master)](https://travis-ci.org/ryeash/assist)
[![Coverage Status](https://coveralls.io/repos/github/ryeash/assist/badge.svg?branch=master)](https://coveralls.io/github/ryeash/assist?branch=master)

assist
======
Lightweight dependency injection framework based on [javax.inject](http://docs.oracle.com/javaee/7/api/javax/inject/package-summary.html "javadoc").

## Basics
In its simplest form:
```java
Assist assist = new Assist();
```
Assist can be used to inject instances of qualifying classes. For instance:
```java
public class FrenchPress {
    public void brew(){
        System.out.println("french press brewing");
    }
}
```
It is possible to get an instance of the FrenchPress class from Assist:
```java
FrenchPress fp = assist.instance(FrenchPress.class);
```
The Assist object will automatically create a `javax.inject.Provider` for FrenchPress based on the default constructor.

The `@Singleton` annotation can be added to FrenchPress to indicate the desired scope:
```java
@Singleton
public class FrenchPress ...
```
In this case Assist will ensure it only ever instantiates one instance of FrenchPress:
```java
FrenchPress fp1 = assist.instance(FrenchPress.class);
FrenchPress fp2 = assist.instance(FrenchPress.class);
assert fp1 == fp2 // true
```

This is the basic function of Assist: create and inject scoped instances of classes (while adhering to the
specifications defined in the javax.inject documentation).

## Application Configuration

Any real application is going to require much more complexity which means a whole bunch of configuration. Assist tries
to simplify the config down to a single Application Configuration class with annotated factory methods.

Let's assume we have a very stupid coffee app, with the CoffeeMaker interface:
```java
public interface CoffeeMaker {
    void brew();
}
```

Coincidentally the FrenchPress class from above already implements the brew() method, so let's update it to:
```java
public class FrenchPress implements CoffeeMaker {
    public void brew(){
        System.out.println("french press brewing");
    }
}
```

And now our Application Configuration class would look something like this:
```java
public class AppConfig {
    @Factory    // makes this method eligible to be turned into a Provider
    @Singleton  // indicates that the Provider should be Singleton scope
    public CoffeMaker coffeeMakerFactory(){
        return new FrenchPress();
    }
}
```

Now we can put the pieces together:
```java
Assist assist = new Assist();
assist.addConfig(AppConfig.class);
CoffeeMaker cm = assist.instance(CoffeeMaker.class);
// cm is the singleton instance of the FrenchPress class created by
// the coffeeMakerFactory() method
cm.brew(); // --> 'french press brewing'
```

If you want to support multiple different coffee makers you'll have to use qualifiers:
```java
public class AppConfig {
    @Factory
    @Singleton
    @Named("his")   // Qualifier that attaches a name to a Provider
    public CoffeMaker frenchPressFactory(){
        return new FrenchPress();
    }

    @Factory
    @Singleton
    @Named("hers")
    public CoffeMaker pourOverFactory(){
        // assuming you have another implementation of the CoffeeMaker
        return new PourOver();
    }
}
```

Let's say we now wanted to inject our Application class:
```java
@Singleton
public class Application {
    @Inject     // indicates Assist should set the field value
    @Named("his")
    private CoffeeMaker his;

    @Inject
    @Named("hers")
    private CoffeeMaker hers;

    @Inject // indicates Assist should call this method after instantiation
    public void makeEveryonesCoffee() {
        hers.brew(); // ladies first
        his.brew();
    }
}
```

Just ask Assist:
```java
Assist assist = new Assist();
assist.addConfig(AppConfig.class);
Application app = assist.instance(Application.class);
// both his and hers coffee makers will be set in app,
// additionally the makeEveryonesCoffee() method will be called
// because it is annotated with @Inject, resulting in an output like:
// > keuring brewing
// > french press brewing
```

### Primary

In case you have multiple qualified factory methods returning the same type,
you can mark one of them as primary:
```java
public class AppConfig {
    @Factory(primary = true)
    @Singleton
    @Named("his")
    public CoffeMaker coffeeMakerFactory(){
        return new FrenchPress();
    }

    ...
}
```
This tells Assist that when an unqualified Provider of the type is requested,
the primary provider should be returned.
```java
CoffeeMaker cm = assist.instance(CoffeeMaker.class);
assert cm instanceof FrenchPress // true
```

Implementation note: internally, marking a qualified provider with 'primary=true' will cause
two providers to be registered for the factory, one with the qualifier, one without.

### Eager

Sometimes you want your @Singletons to not be so lazy. Mark the @Factory as eager to force creation of the
instance during configuration processing.
```java
public class AppConfig {
    @Factory(eager = true)
    @Singleton
    public DAO daoFactory(){
        return new MySqlDAOImpl();
    }
}
```
When this AppConfig is handed to an addConfig method of Assist, the provider for DAO will automatically
be called once to force creation of the singleton.

It is technically possible to mark any @Factory eager, but it really only makes sense for @Singleton scope.

### Lazy
In rare cases it may be necessary to inject a handle to an object before assist is ready to properly wire it; possibly
to avoid dependency or inheritance issues. To solve this, Assist can inject lazy handles to objects:
```java
public class LazilyInjected {
    @Inject // lazy fields still must be marked with @Inject in order to be injected
    @Lazy
    private Provider<Dog> lazyDog;
    
    ...
    
    public void wakeUp(){
        lazyDog.get().wakeup();
    }
}
```
When this class is wired, no provider for Dog needs to be available. Assist will internally create a handle to
get the Dog instance on the first call to Lazy.get(). After the first call the same instance will be returned for each
subsequent call to get().
```java
Assist assist = new Assist();
// we can inject this class safely
LazilyInjected li = assist.instance(LazilyInjected.class);
// now add the Dog instance
assist.setSingelton(Dog.class, new IrishSetter());
// and now the lazy dog can be woken up
li.wakeUp();
```

This should be a rarity and over use may be indicative of underlying architectural problems.

Note: Lazy providers, e.g. `Lazy<Provider<Dog>>`, are not supported.

### @Scan

Simple class path scanning is supported via the @Scan annotation. It will only be evaluated on application configuration 
classes passed into one of the addConfig methods.
```java
@Scan("com.my.base.package")
public class AppConfig {
    ...
}
```
When this class is given to an addConfig method of Assist, the classpath (using whatever class loader the current
thread is using) is scanned recursively for all classes under 'com.my.base.package' that have the @Singleton
annotation. Providers are created for all found classes, and the get() method is called for each, forcing
instantiation. If you need to search for some other annotation type, set the target:
```java
@Scan(value = "com.my.base.package", target = Endpoint.class)
```

### @Aspects
Aspect Oriented Programming (AOP) is supported on @Factory methods with the use of the @Aspects annotation.
You can, for example, add a Logging aspect to a provided instance:
First define your aspect:
```java
public class LoggingAspect extends Aspect {

    private Logger log;

    @Override
    public void init(Object instance) {
        // when overriding init, don't forget to call super
        super.init(instance);
        this.log = LoggerFactory.getLogger(instance.getClass());
    }

    // before every method invocation, log a message
    @Override
    public void pre(Invocation invocation) throws Throwable {
        log.info("entering {}", invocation);
    }

    // after every method invocation, log a message
    @Override
    public void post(Invocation invocation) throws Throwable {
        log.info("exiting {}", invocation);
    }
}
```
Then apply the aspect to a factory method using @Aspects(LoggingAspect.class):
```java
@Factory
@Named("aspect1")
@Aspects(LoggingAspect.class)
public CoffeeMaker aspectedFrenchPress1() {
    return new FrenchPress();
}
```
Multiple aspects can be used on a single target:
```java
@Factory
@Named("aspect2")
@Singleton
@Aspects({LoggingAspect.class, TimingAspect.class})
public CoffeeMaker aspectedFrenchPress2() {
    return new FrenchPress();
}
```
One thing to keep in mind when using multiple aspects is that pre and post methods will be called 
for all aspects but only the exec method of the last aspect in the array will be called. Also
the call order of aspects may seem a little unusual at first, pre methods are called first to last, exec is only called for
the last aspect, then post methods are called last to first. So, in the previous example the call order for the aspects will be:
```
LoggingAspect:pre
TimingAspect:pre
TimingAspect:exec
TimingAspect:post
LoggingAspect:post
```

Assist uses `java.lang.reflect.Proxy` to join the aspect classes with the provided types and as such the @Aspects
annotation is only usable on methods that return an interface type.

### @Property
Assist has built-in property support using the [@Property](src/main/java/vest/assist/annotations/Property.java) and 
[ConfigurationFacade](src/main/java/vest/assist/conf/ConfigurationFacade.java) classes.

To use the @Property annotation, an unqualified ConfigurationFacade must be made available for injection. For example,
via a @Factory method in an application configuration class:
```java
@Factory
@Singleton
public ConfigurationFacade configurationFacadeFactory() {
    // ordering of sources is relevant:
    // sources will be polled for properties in the order they were
    // added, and the first non-empty value from a source will be returned
    return ConfigurationFacade.build()
            .system() // get values from System.getProperty(...)
            .file("../conf/app-override.properties")
            .file("../conf/app.properties")
            .enableEnvironments()
            .enableCaching()
            .enableMacros()
            .finish();
}
```
Using the @Property annotation without an available ConfigurationFacade will cause RuntimeExceptions during injection.

With the ConfigurationFacade created and available, fields and parameters can be injected from configuration sources.
```java
@Singleton
public class SomeComponent {

    @Property("string")
    private String str;
    
    @Inject // the @Inject annotation is optional when @Property is present
    @Property("boolean")
    public Boolean bool; // any type with a static valueOf(String) method is supported

    @Property("integer")
    public int integer;

    @Property("numbers.list")
    public List<Double> numbers; // List, Sets, SortedSets

    @Inject
    private void setProps(@Property("enum")SomeEnum someEnum){
        // ...
    }
    
    // if you just want to use the ConfigurationFacade directly
    @Inject
    private void configureIt(ConfigurationFacade conf){
        // ... configure it ...
    }
}
```
By default, properties are required, a RuntimeException will be thrown if a value can not be found
during injection. Individual properties can be made optional by setting required=false, e.g.:
```@Property(value = "some.property", required = false)```


### @Scheduled
Methods in injected instances are schedulable using the @Scheduled annotation. Internally, a ScheduledExecutorService is
used to manage the scheduling and execution of the tasks. The executor must be made available to the Assist instance
performing the injection.

For example, we can configure the executor in a factory method:
```java
public class AppConfig {
    @Factory
    @Singleton // it is highly recommended that the executor be made a singleton
    public ScheduledExecutorService scheduledExecutorServiceFactory() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
```
Using the @Scheduled annotation, set a method to be scheduled by Assist.
```java
public class ObjectWithATask {
    @Scheduled(name = "pointless-task", type = FIXED_RATE, period = 3, executions = 4)
    private void myScheduledTask(CoffeeMaker cm) {
        log.info("running task");
    }
}
```
Now wire an instance and the task will be scheduled.
```java
Assist assist = new Assist();
assist.addConfig(AppConfig.class);
assist.instance(ObjectWithATask.class);
```
See the documentation in @Scheduled for more details.


### Explicit Implementation Definition

It is possible to define the implementation of interfaces/abstract
classes directly, rather than using an application configuration class.
```java
Assist assist = new Assist();
// this declares that for the CoffeeMaker interface, we want to use the PourOver class.
assist.addImplementingClass(CoffeeMaker.class, PourOver.class);
// we now have a provider for CoffeeMaker
assert assist.hasProvider(CoffeeMaker.class);
// and as expected when we get a CoffeeMaker instance, it's a PourOver
assert assist.instance(CoffeeMaker.class).getClass() == PourOver.class;
```


### Shutdown Container

All objects instantiated by Assist that are AutoCloseable will be tracked by a WeakHashMap and when
```assist.close()``` is called, they will be closed as appropriate.

## Extensibility

### ScopeFactory

Support for additional Provider scopes (beyond just Singleton) is handled with the 
[ScopeFactory](src/main/java/vest/assist/ScopeFactory.java) interface.
See [ThreadLocal](src/main/java/vest/assist/annotations/ThreadLocal.java) and 
[ThreadLocalScopeFactory](src/main/java/vest/assist/provider/ThreadLocalScopeFactory.java) 
for an example on how to create a ScopeFactory.

### InstanceInterceptor

InstanceInterceptors are used (in general) to inject fields or methods of a class after an instance has been created.
For example, the [InjectAnnotationInterceptor](src/main/java/vest/assist/provider/InjectAnnotationInterceptor.java) 
injects the @Inject fields and methods of a class.

To add, for example, a log injector:

Define a @Log annotation:
```java
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
}
```

Define the interceptor:
```java
public class LogInjector implements InstanceInterceptor {

    @Override
    public void intercept(Object instance) {
        Reflector.of(instance).forAnnotatedFields(Log.class, (logAnnotation, field) -> {
            try {
                field.set(instance, LoggerFactory.getLogger(instance.getClass()));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("error setting log: " + field, e);
            }
        });
    }
}
```

Register the instance interceptor:
```java
assist.register(new LogInjector());
```

Now, any time a Provider creates an object instance with fields annotated with @Log, those fields will be set to a Logger
created using the Slf4j LoggerFactory.

### ValueLookup

Custom handling of @Inject fields and methods can be performed by adding additional ValueLookup implementations to the
assist instance. A ValueLookup is tasked with finding the value that should be used to set an @Inject marked Field
or an @Inject marked method's Parameter values. During injection processing Assist will iterate through all
registered ValueLookups (in prioritized order) until one of them returns a non-null value for the target.

A new ValueLookup can be registered with:
```java
assist.register(new CustomValueLookup());
```

### Prioritized
The configuration related interfaces in the assist module all implement the Prioritized interface. This provides a way to
control the order of execution for InstanceInterceptors and ValueLookups. The default priority
is 1000. There's no simple rule for what priority should be set to for custom configurations, but you can use
assist.toString() to get a diagnostic printout of what Assist has registered and the order of execution.

## Best Practices

Configure your Assist instance (just one) as early as possible in the main thread (ideally it's the very first thing that happens).
It's much better to have your app fail early and kill the JVM than at some random point down the road when it tries to
configure a provider in a request thread and it leaves everything in a walking wounded state.
