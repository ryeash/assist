package vest.assist;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.assist.annotations.Aspects;
import vest.assist.annotations.Factory;
import vest.assist.annotations.Scheduled;
import vest.assist.app.LoggingAspect;
import vest.assist.app.Teapot;
import vest.assist.provider.AdHocProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Set;

public class AssistNegativeTest extends Assert {

    Assist assist;

    @BeforeClass(alwaysRun = true)
    public void initializeAssit() {
        assist = new Assist();
        assist.addConfig("vest.assist.app.AppConfig");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testConfigFailure1() {
        assist.addConfig("tester.no.Thing");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testConfigFailure2() {
        assist.addConfig(Number.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testMissingProvider() {
        assist.providerFor(Set.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDoubleSetProvider() {
        Provider<Teapot> p = assist.providerFor(Teapot.class);
        assist.setProvider(Teapot.class, null, new AdHocProvider<>(p.get()));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void invalidFactory() {
        assist.addConfig(new Object() {
            @Factory
            public void voidFactory() {
            }
        });
    }

    public static class ExceptionMethod {
        @Inject
        public void throwsException() {
            throw new IllegalArgumentException("I'm supposed to do this");
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void injectMethodException() {
        assist.instance(ExceptionMethod.class);
    }

    @Test
    public void badMethodFactory() {
        Assist a = new Assist();
        a.addConfig(new Object() {

            @Factory
            public String nullFactory() {
                return null;
            }

            @Factory
            public Integer errorFactory() {
                throw new IllegalArgumentException("bad factory");
            }
        });

        assertThrows(NullPointerException.class, () -> a.instance(String.class));
        assertThrows(RuntimeException.class, () -> a.instance(Integer.class));
    }

    public static class AbhorrentConstructor {
        @Inject
        public AbhorrentConstructor(List stuff) {

        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void badConstructorArg() {
        assist.instance(AbhorrentConstructor.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void illegalAspectUsage() {
        Assist a = new Assist();
        a.addConfig(new Object() {
            @Factory
            @Aspects(LoggingAspect.class)
            public String toastFactory() {
                return "toast";
            }
        });
        a.instance(String.class);
    }


    @Test
    public void scheduleErrors() {
        assist.inject(new Object() {
            @Scheduled(name = "my-task", type = Scheduled.RunType.FIXED_DELAY, period = 100, executions = 1)
            public void task() throws Exception {
                throw new Exception("oh no!");
            }
        });

        assertThrows(RuntimeException.class, () -> {
            assist.inject(new Object() {
                @Scheduled(type = Scheduled.RunType.FIXED_RATE, period = -1)
                public void task() {

                }
            });
        });

        assertThrows(RuntimeException.class, () -> {
            assist.inject(new Object() {
                @Scheduled(type = Scheduled.RunType.FIXED_DELAY, period = -1)
                public void task() {

                }
            });
        });
    }
}
