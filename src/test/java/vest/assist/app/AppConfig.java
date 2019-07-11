package vest.assist.app;

import org.testng.Assert;
import vest.assist.Assist;
import vest.assist.annotations.Aspects;
import vest.assist.annotations.Eager;
import vest.assist.annotations.Factory;
import vest.assist.annotations.Primary;
import vest.assist.annotations.Scan;
import vest.assist.annotations.ThreadLocal;
import vest.assist.conf.ConfigurationFacade;
import vest.assist.util.ExecutorBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Scan("vest.assist.app")
public class AppConfig extends BaseAppConfig {

    @Inject
    public AppConfig(Assist assist) {
        Assert.assertNotNull(assist);
    }

    @Factory
    public InputStream someInput(Properties properties) {
        Assert.assertNotNull(properties);
        return new ByteArrayInputStream(new byte[0]);
    }

    @Factory
    @Named("bais")
    public ByteArrayInputStream someBais() {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Factory
    @Primary
    @Named("frenchPress")
    @ThreadLocal
    public CoffeeMaker frenchPress() {
        return new FrenchPress();
    }

    @Factory
    @Named("keurig")
    public CoffeeMaker keurig() {
        return new Keurig();
    }

    @Factory
    @Leather(color = Leather.Color.BLACK)
    public Coosie blackLeatherCoosie() {
        return new Coosie("black");
    }

    @Factory
    @Eager
    @Leather(color = Leather.Color.RED)
    @Singleton
    public Coosie redLeatherCoosie() {
        return new Coosie("red");
    }

    @Factory
    @Named("aspect1")
    @Aspects(LoggingAspect.class)
    public CoffeeMaker aspectedFrenchPress1() {
        return new FrenchPress();
    }

    @Factory
    @Named("aspect2")
    @Singleton
    @Aspects({LoggingAspect.class, TimingAspect.class})
    public CoffeeMaker aspectedFrenchPress2() {
        return new FrenchPress();
    }

    @Factory
    @Singleton
    public ScheduledExecutorService scheduledExecutorServiceFactory() {
        return ExecutorBuilder.newExecutor()
                .setDaemonize(true)
                .setThreadNamePrefix("scheduled-pool-")
                .setContextClassLoader(ClassLoader.getSystemClassLoader())
                .setUncaughtExceptionHandler((thread, error) -> error.printStackTrace())
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .scheduledExecutor(1);
    }

    @Factory
    @Singleton
    @Named("alternate")
    public ScheduledExecutorService alternateScheduledExecutoreServiceFactory() {
        return ExecutorBuilder.newExecutor()
                .setDaemonize(true)
                .setThreadNamePrefix("alt-scheduled-pool-")
                .setContextClassLoader(ClassLoader.getSystemClassLoader())
                .setUncaughtExceptionHandler((thread, error) -> error.printStackTrace())
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .scheduledExecutor(1);
    }

    @Factory
    @Singleton
    public TCMultipleDependenciesSatisfied multipleDeps() {
        return new TCMultipleDependenciesSatisfied();
    }

    @Factory
    @Singleton
    public ConfigurationFacade configurationFacadeFactory() {
        return ConfigurationFacade.build()
                .classpathFile("test.conf")
                .enableEnvironments()
                .enableCaching()
                .enableInterpolation()
                .finish();
    }
}
