package vest.assist.util;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builder class for creating different types of {@link ExecutorService} services.
 */
public final class ExecutorBuilder {

    public static ExecutorBuilder newExecutor() {
        return new ExecutorBuilder();
    }

    private boolean daemonize = false;
    private String threadPrefix = "thread-";
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;
    private ClassLoader classLoader = null;
    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    /**
     * Start building a new {@link ExecutorService}.
     */
    public ExecutorBuilder() {
    }

    /**
     * Set the daemon flag for the threads created for the executor.
     *
     * @param daemonize the daemon flag setting for threads
     * @return this builder
     * @default false
     * @see Thread#setDaemon(boolean)
     */
    public ExecutorBuilder setDaemonize(boolean daemonize) {
        this.daemonize = daemonize;
        return this;
    }

    /**
     * Set the thread name prefix for threads created for the executor. The thread prefix will be suffixed with the
     * thread number in the pool, creating the name of the thread.
     * Example: <code>setThreadNamePrefix("background-")</code> will result in thread names like, 'background-2'.
     *
     * @param threadPrefix the thread prefix
     * @return this builder
     * @default "thread-"
     * @see Thread#setName(String)
     */
    public ExecutorBuilder setThreadNamePrefix(String threadPrefix) {
        if (threadPrefix == null || threadPrefix.isEmpty()) {
            throw new IllegalArgumentException("thread prefix must not be empty");
        }
        this.threadPrefix = threadPrefix;
        return this;
    }

    /**
     * Set the {@link java.lang.Thread.UncaughtExceptionHandler} for the threads created for the executor.
     *
     * @param uncaughtExceptionHandler the uncaught exception handler
     * @return this builder
     * @default null
     * @see Thread#setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)
     */
    public ExecutorBuilder setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    /**
     * Set the context {@link ClassLoader} for the threads created for the executor.
     *
     * @param classLoader the context class loader
     * @return this builder
     * @default null
     * @see Thread#setContextClassLoader(ClassLoader)
     */
    public ExecutorBuilder setContextClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Set the {@link RejectedExecutionHandler} for the executors created by this builder.
     *
     * @param rejectedExecutionHandler the rejected execution handler
     * @return this builder
     * @default {@link ThreadPoolExecutor.AbortPolicy}
     * @see RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)
     * @see ThreadPoolExecutor#setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public ExecutorBuilder setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = Objects.requireNonNull(rejectedExecutionHandler);
        return this;
    }

    /**
     * Create a new fixed size {@link ThreadPoolExecutor} using the current thread factory settings.
     *
     * @param nThreads the number of threads to use in the thread pool
     * @return a new fixed size {@link ThreadPoolExecutor}
     */
    public ExecutorService threadPoolExecutor(int nThreads) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException("thread count must be greater than zero");
        } else {
            return threadPoolExecutor(nThreads, nThreads, 60, new LinkedBlockingDeque<>());
        }
    }

    /**
     * Create a new variable size {@link ThreadPoolExecutor} using the current thread factory settings.
     *
     * @param minThreads the minimum number of threads to maintain in the thread pool
     * @param maxThreads the maximum number of threads to allow in the thread pool
     * @return a new variable size {@link ExecutorService}
     */
    public ExecutorService threadPoolExecutor(int minThreads, int maxThreads) {
        return threadPoolExecutor(minThreads, maxThreads, 60, new SynchronousQueue<>());
    }

    /**
     * Create a new {@link ThreadPoolExecutor} using the current thread factory settings.
     *
     * @param minThreads       the minimum number of threads to maintain in the thread pool
     * @param maxThreads       the maximum number of threads to allow in the thread pool
     * @param keepAliveSeconds the maximum seconds an idle thread will wait before terminating
     * @param workQueue        the queue to use for holding tasks before they are executed.  This queue will hold only
     *                         the {@code Runnable} tasks submitted by the {@code execute} method.
     * @return a new {@link ThreadPoolExecutor}
     */
    public ExecutorService threadPoolExecutor(int minThreads, int maxThreads, long keepAliveSeconds, BlockingQueue<Runnable> workQueue) {
        if (minThreads <= 0) {
            throw new IllegalArgumentException("minimum thread count must be greater than zero");
        }
        if (maxThreads <= 0 || maxThreads < minThreads) {
            throw new IllegalArgumentException("maximum thread count must be greater than or equal to minimum thread count");
        }
        if (keepAliveSeconds < 0) {
            throw new IllegalArgumentException("the keep alive time must be greater than or equal to zero (ideally greater than zero)");
        }
        Objects.requireNonNull(workQueue, "the work queue may not be null");
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(minThreads, maxThreads, keepAliveSeconds, TimeUnit.SECONDS,
                workQueue, tf(), rejectedExecutionHandler);
        threadPoolExecutor.allowCoreThreadTimeOut(false);
        threadPoolExecutor.prestartAllCoreThreads();
        return Executors.unconfigurableExecutorService(threadPoolExecutor);
    }

    /**
     * Create a new {@link ScheduledExecutorService} using the current settings.
     *
     * @param corePoolSize the number of threads to keep in the pool
     * @return a new {@link ScheduledExecutorService}
     * @see ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory, RejectedExecutionHandler)
     */
    public ScheduledExecutorService scheduledExecutor(int corePoolSize) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize, tf(), rejectedExecutionHandler);
        scheduledThreadPoolExecutor.allowCoreThreadTimeOut(false);
        scheduledThreadPoolExecutor.prestartAllCoreThreads();
        scheduledThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        return Executors.unconfigurableScheduledExecutorService(scheduledThreadPoolExecutor);
    }

    /**
     * Create a new {@link ForkJoinPool} using the current settings.
     *
     * @param parallelism the parallelism level
     * @return a new fork join pool
     * @see ForkJoinPool#ForkJoinPool(int, ForkJoinPool.ForkJoinWorkerThreadFactory, Thread.UncaughtExceptionHandler, boolean)
     */
    public ExecutorService forkJoinPool(int parallelism) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism, tf(), uncaughtExceptionHandler, true);
        return Executors.unconfigurableExecutorService(forkJoinPool);
    }

    private CustomThreadFactory tf() {
        return new CustomThreadFactory(daemonize, threadPrefix, uncaughtExceptionHandler, classLoader);
    }

    public static final class CustomThreadFactory implements ThreadFactory, ForkJoinPool.ForkJoinWorkerThreadFactory {

        private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        private final AtomicInteger counter = new AtomicInteger(0);
        private final boolean daemonize;
        private final String threadPrefix;
        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
        private final ClassLoader classLoader;

        public CustomThreadFactory(boolean daemonize, String threadPrefix, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, ClassLoader classLoader) {
            this.daemonize = daemonize;
            this.threadPrefix = threadPrefix;
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;
            this.classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
        }

        @Override
        public Thread newThread(Runnable r) {
            return configure(defaultThreadFactory.newThread(r));
        }

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return configure(ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool));
        }

        private <T extends Thread> T configure(T thread) {
            thread.setDaemon(daemonize);
            thread.setName(threadPrefix + counter.incrementAndGet());
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            thread.setContextClassLoader(classLoader);
            return thread;
        }
    }
}
