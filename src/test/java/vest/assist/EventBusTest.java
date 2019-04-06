package vest.assist;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.assist.event.EventBus;
import vest.assist.event.EventListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EventBusTest extends Assert {

    private EventBus eventBus = new EventBus(Executors.newFixedThreadPool(3));
    private static String testStr = "this is the message";

    public static class TestCase1 {

        public CountDownLatch cd = new CountDownLatch(1);

        @EventListener
        public void handleStringMessage(String message) {
            cd.countDown();
            assertEquals(message, testStr);
        }
    }

    @Test
    public void basicTest() throws InterruptedException {
        TestCase1 tc1 = new TestCase1();
        eventBus.register(tc1);
        eventBus.publish(testStr);
        assertTrue(tc1.cd.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void unregister() {
        TestCase1 tc1 = new TestCase1();
        eventBus.register(tc1);
        eventBus.unregister(tc1);
        eventBus.publish(testStr);
        assertEquals(tc1.cd.getCount(), 1L);
    }

    public static class TestCase2 {

        CountDownLatch cd = new CountDownLatch(2);

        @EventListener
        public void handleInteger(Integer i) {
            System.out.println("int: " + i);
            cd.countDown();
            assertEquals(i.intValue(), 123);
        }

        @EventListener
        public void handleLong(Long l) {
            System.out.println("long: " + l);
            cd.countDown();
            assertEquals(l.longValue(), 1234L);
        }

    }

    @Test
    public void multiTypes() throws InterruptedException {
        TestCase2 tc2 = new TestCase2();
        eventBus.register(tc2);
        eventBus.publish(123);
        eventBus.publish(1234L);
        assertTrue(tc2.cd.await(1, TimeUnit.SECONDS));
    }

    public static class TestCaseParallelism {

        public static AtomicLong events = new AtomicLong();
        public static Pattern pattern = Pattern.compile("\\s+");

        @EventListener
        public void event1(String message) {
            events.incrementAndGet();
            String[] split = pattern.split(message);
            List<String> strings = Arrays.asList(split);
            assertTrue(strings.contains("this"));
        }

        @EventListener
        public void event2(String message) {
            events.incrementAndGet();
            String[] split = pattern.split(message);
            List<String> strings = Arrays.asList(split);
            assertTrue(strings.contains("this"));
        }

        @EventListener
        public void event3(String message) {
            events.incrementAndGet();
            String[] split = pattern.split(message);
            List<String> strings = Arrays.asList(split);
            assertTrue(strings.contains("this"));
        }
    }

    @Test
    public void parallelism() throws InterruptedException {
        EventBus eb = new EventBus(Executors.newCachedThreadPool());

        long start = System.nanoTime();
        ConcurrentLinkedDeque<TestCaseParallelism> collect = IntStream.range(0, 1500)
                .mapToObj(i -> new TestCaseParallelism())
                .peek(eb::register)
                .collect(Collectors.toCollection(ConcurrentLinkedDeque::new));
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));

        AtomicLong total = new AtomicLong(0);
        AtomicLong unT = new AtomicLong(0);
        start = System.nanoTime();
        IntStream.range(0, 1500)
                .parallel()
                .forEach(i -> {
                    TestCaseParallelism tc = collect.removeFirst();
                    long u = System.nanoTime();
                    eb.unregister(tc);
                    unT.accumulateAndGet(System.nanoTime() - u, (a, b) -> a + b);

                    long s = System.nanoTime();
                    eb.publish("this is the string " + i);
                    total.accumulateAndGet(System.nanoTime() - s, (a, b) -> a + b);
                });
        System.out.println("u: " + unT.get());
        System.out.println("p: " + total.get());

        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
        System.out.println("---");
        Thread.sleep(1000);
        System.out.println(TestCaseParallelism.events.get());
        System.out.println("----------------------");
    }
}
