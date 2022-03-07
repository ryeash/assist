package vest.assist.test;

import org.testng.annotations.Test;

@TestConfiguration(scan = "vest.assist.test")
public class MiscTest extends AssistBaseTest {

    @Test
    public void init() {
        log.info("{}", assist());
    }

    @Test
    public void basicFactory() {
        String instance = assist().instance(String.class);
        assertEquals(instance, "stringFactory");
    }
}
