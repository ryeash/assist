package demo.app;

import demo.app.model.Message;
import org.testng.annotations.Test;
import vest.assist.jpa.JPAContext;
import vest.assist.test.AssistBaseTest;
import vest.assist.test.TestConfiguration;

@TestConfiguration(scan = "demo.app")
public class JpaTest extends AssistBaseTest {

    public JPAContext jpa() {
        return assist().instance(JPAContext.class);
    }

    @Test
    public void init() {
        jpa().inTransaction(em -> {
            Message message = new Message();
            message.setTimestamp(System.currentTimeMillis());
            message.setUserId(1L);
            message.setMessage("message");
            em.persist(message);
        });

        jpa().managed(em -> {
            em.createQuery("select m from Message m", Message.class)
//                    .setParameter("message", "message")
                    .getResultStream()
                    .forEach(m -> log.info("{}", m));
        });
    }
}
