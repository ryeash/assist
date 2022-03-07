package vest.assist.test;

import vest.assist.Assist;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class AssistManager implements AutoCloseable {

    private final Map<TestConfiguration, Assist> cache = new ConcurrentHashMap<>(4, 1F, 1);

    public Assist getOrCreate(TestConfiguration testConfiguration) {
        return cache.computeIfAbsent(testConfiguration, this::build);
    }

    private Assist build(TestConfiguration testConfiguration) {
        return new Assist(testConfiguration.scan());
    }

    @Override
    public void close() {
        for (Assist assist : cache.values()) {
            assist.close();
        }
    }
}
