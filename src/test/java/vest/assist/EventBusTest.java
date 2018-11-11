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
import java.util.function.Consumer;
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

        CountDownLatch cd = new CountDownLatch(4);

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

        @EventListener
        public void handleNumber(Number n) {
            System.out.println("number: " + n);
            cd.countDown();
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

    @Test
    public void consumer() throws InterruptedException {
        CountDownLatch cd = new CountDownLatch(1);
        eventBus.register(String.class, str -> {
            cd.countDown();
            assertEquals(str, testStr);
        });
        eventBus.publish(testStr);
        assertTrue(cd.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void unregisterConsumer() {
        CountDownLatch cd = new CountDownLatch(1);
        Consumer<String> consumer = str -> {
            cd.countDown();
            assertEquals(str, testStr);
        };
        eventBus.register(String.class, consumer);
        eventBus.unregister(consumer);
        eventBus.publish(testStr);
        assertEquals(cd.getCount(), 1L);
    }

    public static class TestCaseParallelism {

        @EventListener
        public void event1(String message) {
            String[] split = message.split("\\s+");
            List<String> strings = Arrays.asList(split);
            assertTrue(strings.contains("this"));
        }

        @EventListener
        public void event2(String message) {
            String[] split = message.split("\\s+");
            List<String> strings = Arrays.asList(split);
            assertTrue(strings.contains("this"));
        }

        @EventListener
        public void event3(String message) {
            String[] split = message.split("\\s+");
            List<String> strings = Arrays.asList(split);
            assertTrue(strings.contains("this"));
        }
    }

    @Test
    public void parallelism() {
        ConcurrentLinkedDeque<TestCaseParallelism> collect = IntStream.range(0, 1500)
                .mapToObj(i -> new TestCaseParallelism())
                .peek(eventBus::register)
                .collect(Collectors.toCollection(ConcurrentLinkedDeque::new));

        IntStream.range(0, 1500)
                .parallel()
                .forEach(i -> {
                    TestCaseParallelism tc = collect.removeFirst();
                    eventBus.unregister(tc);
                    eventBus.publish("this is the string " + i);
                });
    }
}
