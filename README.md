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
In this case the Assist will ensure it only ever instantiates one instance of FrenchPress:
```java
FrenchPress fp1 = assist.instance(FrenchPress.class);
FrenchPress fp2 = assist.instance(FrenchPress.class);
assert fp1 == fp2 // true
```

This is the basic function of the Assist: create and inject scoped instances of classes (while adhering to the
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
    public CoffeMaker keurigFactory(){
        // assuming you have another implementation of the CoffeeMaker
        return new Keurig();
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

Just ask the assist:
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

In instances where you have multiple qualified factory methods returning the same type,
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
Primary tells the Assist that when an unqualified Provider of the type is requested,
that provider should be returned.
```java
CoffeeMaker cm = assist.instance(CoffeeMaker.class);
assert cm instanceof FrenchPress // true
```

Implementation note: internally, marking a qualified provider with 'primary=true' will cause
two providers to be registered for the factory, one with the qualifier, one without.

### Eager

Sometimes you want your @Singletons to not be so lazy. Mark the @Factory as eager to force creation of the
instance during application configuration.
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

### @Scan

Simple class path scanning is supported via the @Scan annotation. It will only be evaluated on application configuration 
classes passed into one of the addConfig methods.
```java
@Scan("com.my.base.package")
public class AppConfig {

}
```
When this class is given to an addConfig method of Assist, the classpath (using whatever class loader the current
thread is using) is scanned recursively for all classes under 'com.my.base.package' that have the @Singleton
annotation. Providers are created for all found classes, and the get() method is called for each, forcing
instantiation. If you need to search for some other annotation type, you can:
```java
@Scan(value = "com.my.base.package", target = Endpoint.class)
```

### @Aspect
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

Assist uses java.lang.reflect.Proxy class to join the aspect classes with the interfaces and as such the @Aspects
annotation is only usable on methods that return an interface type.

### Explicit Implementation Definition

It is possible (and recommended with simple application architectures) to define the implementation of interfaces/abstract
classes directly, rather than using an application configuration class.
```java
Assist assist = new Assist();
// this says that for the CoffeeMaker interface, we want to use the Keurig class.
assist.addImplementingClass(CoffeeMaker.class, Keurig.class);
// we now have a provider for CoffeeMaker
assert assist.hasProvider(CoffeeMaker.class);
// and as expected when we get a CoffeeMaker instance, it's a Keurig
assert assist.instance(CoffeeMaker.class).getClass() == Keurig.class; // true
```

### Shutdown Container

All objects instantiated by the Assist that are AutoCloseable will be tracked by a WeakHashMap and when
```assist.close()``` is called, they will be closed as appropriate.

## Extensibility

### ScopeProvider

Support for additional Provider scopes (beyond just Singleton) is handled with the ScopeProvider interface.
For instance, if you wanted to add a @PerRequest scope:

Create the scope:
```java
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
@Scope // <-- Scopes MUST have the 'parent' @Scope annotation
public @interface Request {
}
```

Create the ScopeProvider:
```java
public class RequestScopeProvider<T> implements ScopeProvider<T> {

    private ThreadLocal<T> threadLocal = new ThreadLocal<>();

    @Override
    public T scope(Provider<T> provider) {
        if (threadLocal.get() == null) {
            synchronized (this) {
                if (threadLocal.get() == null) {
                    threadLocal.set(provider.get());
                }
            }
        }
        return threadLocal.get();
    }
}
```

Register it with the Assist:
```java
Assist assist = new Assist();
assist.registerScope(PerRequest.class, RequestScopeProvider.class);
```

Now class and factory method providers with the @PerRequest scope annotation will create one instance per request thread.

Note: ScopeProvider implementations must be inject compatible, see:[@Inject](http://docs.oracle.com/javaee/7/api/javax/inject/Inject.html).
Internally when creating a provider chain, the ScopeProvider itself is created using the
injection mechanism which will throw errors if the implementation can't be injected.


### InstanceInterceptor

InstanceInterceptors are used (in general) to inject fields or methods of a class after an instance has been created.
For example, the InjectAnnotationInterceptor injects the @Inject fields, and methods of a class.

To add, for example, a log injector:

Define a @Log annotation:
```java
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
}
```

Add the instance interceptor:
```java
assist.addInstanceInterceptor(instance -> {
    Reflector.of(instance).forAnnotatedFields(Log.class, (logAnnotation, field) -> {
        try {
            field.set(instance, LoggerFactory.getLogger(instance.getClass()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("error setting log: " + field, e);
        }
    });
});
```

Now, any time a Provider creates an object instance with fields annotated with @Log, those fields will be set to a Logger
created using the Slf4j LoggerFactory.

### ValueLookup

Custom handling of @Inject fields and methods can be performed by adding additional ValueLookup implementations to the
assist instance. A ValueLookup is tasked with finding the value that should be used to set an @Inject marked Field
or an @Inject marked method's Parameter values. During injection processing the Assist instance will iterate through all
registered ValueLookups (in prioritized order) until one of them returns a non-null value for the target.

A new ValueLookup can be registered with:
```java
assist.addValueLookup(new CustomValueLookup());
```

### Prioritized
The configuration related interfaces in the assist module all implement the Prioritized interface. This provides a way to
control the order of execution for InstanceInterceptors and ValueLookups. The default priority
is 1000. There's no simple rules for what priority should be set to for custom configurations, but you can use
assist.toString() to get a diagnostic printout of what the Assist has registered and the order of execution.

## Best Practices

Configure the Assist instance (just one) as early as possible in the main thread (ideally it's the very first thing that happens).
It's much better to have your app fail early and kill the JVM than at some random point down the road when it tries to
configure a provider in a request thread and it leaves everything in a walking wounded state.
