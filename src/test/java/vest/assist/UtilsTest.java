package vest.assist;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.assist.util.ExecutorBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

public class UtilsTest extends Assert {

    @Test
    public void threadPool() {
        executorProof(ExecutorBuilder.newExecutor()
                .setDaemonize(true)
                .setThreadNamePrefix("testpool-")
                .setContextClassLoader(ClassLoader.getSystemClassLoader())
                .setUncaughtExceptionHandler((thread, error) -> error.printStackTrace())
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .threadPoolExecutor(1));

        executorProof(ExecutorBuilder.newExecutor()
                .setDaemonize(true)
                .setThreadNamePrefix("testpool-")
                .setContextClassLoader(ClassLoader.getSystemClassLoader())
                .setUncaughtExceptionHandler((thread, error) -> error.printStackTrace())
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .threadPoolExecutor(5, 100));
    }

    @Test
    public void scheduled() {
        executorProof(ExecutorBuilder.newExecutor()
                .setDaemonize(true)
                .setThreadNamePrefix("testpool-")
                .setContextClassLoader(ClassLoader.getSystemClassLoader())
                .setUncaughtExceptionHandler((thread, error) -> error.printStackTrace())
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .scheduledExecutor(1));

        executorProof(ExecutorBuilder.newExecutor()
                .setDaemonize(true)
                .setThreadNamePrefix("testpool-")
                .setContextClassLoader(ClassLoader.getSystemClassLoader())
                .setUncaughtExceptionHandler((thread, error) -> error.printStackTrace())
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .scheduledExecutor(1, 10));
    }

    @Test
    public void forkJoin() {
        executorProof(ExecutorBuilder.newExecutor()
                .setDaemonize(true)
                .setThreadNamePrefix("testpool-")
                .setContextClassLoader(ClassLoader.getSystemClassLoader())
                .setUncaughtExceptionHandler((thread, error) -> error.printStackTrace())
                .setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .forkJoinPool(1));
    }

    @Test
    public void executorBuilderErrors() {
        assertThrows(IllegalArgumentException.class, () -> {
            ExecutorBuilder.newExecutor()
                    .threadPoolExecutor(-1, -1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExecutorBuilder.newExecutor()
                    .threadPoolExecutor(1, -1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExecutorBuilder.newExecutor()
                    .threadPoolExecutor(1, 1, -1, new LinkedBlockingDeque<>());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExecutorBuilder.newExecutor()
                    .setThreadNamePrefix(null)
                    .threadPoolExecutor(1);
        });
    }

    private void executorProof(ExecutorService executor) {
        try {
            Assert.assertFalse(executor instanceof ThreadPoolExecutor);
            Assert.assertFalse(executor instanceof ScheduledThreadPoolExecutor);
            Assert.assertFalse(executor instanceof ForkJoinPool);

            IntStream.range(0, 100)
                    .parallel()
                    .forEach(i -> {
                        try {
                            assertEquals(executor.submit(() -> i).get(), (Integer) i);
                        } catch (Exception e) {
                            fail("error", e);
                        }
                    });
        } catch (Exception e) {
            fail("error", e);
        }
    }
}
