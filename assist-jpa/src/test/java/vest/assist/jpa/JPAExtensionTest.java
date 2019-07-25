package vest.assist.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import vest.assist.Assist;
import vest.assist.jpa.app.Message;

public class JPAExtensionTest {

    private static Logger log = LoggerFactory.getLogger(JPAExtension.class);

    Assist assist;

    @BeforeTest(alwaysRun = true)
    public void testSetup() {
        assist = new Assist("vest.assist.jpa.app");
    }

    @Test
    public void init() {
        JPAContext instance = assist.instance(JPAContext.class);
        instance.inTransaction(em -> {
            Message message = new Message();
            message.setTimestamp(System.currentTimeMillis());
            message.setUserId(1L);
            message.setMessage("message");
            em.persist(message);
        });

        instance.managed(em -> {
            em.createQuery("select m from Message m", Message.class)
//                    .setParameter("message", "message")
                    .getResultStream()
                    .forEach(m -> log.info("{}", m));
        });
    }
}
