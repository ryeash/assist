package vest.assist.app;

import org.testng.Assert;
import vest.assist.Assist;
import vest.assist.annotations.Aspects;
import vest.assist.annotations.Factory;
import vest.assist.annotations.Scan;
import vest.assist.annotations.ThreadLocal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Scan("vest.assist.app")
public class AppConfig extends BaseAppConfig {

    @Inject
    public AppConfig(Assist assist) {
        Assert.assertNotNull(assist);
    }

    @Factory
    public InputStream someInput(Properties properties) {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Factory
    @Named("bais")
    public ByteArrayInputStream someBais() {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Factory(primary = true)
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

    @Factory(eager = true)
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
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Factory
    @Singleton
    public OneClassForMultipleDependencies multipleDeps() {
        return new OneClassForMultipleDependencies();
    }
}
