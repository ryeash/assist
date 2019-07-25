package demo.app;

import demo.app.model.Message;
import org.testng.annotations.Test;
import vest.assist.jpa.JPAContext;

import javax.inject.Inject;

public class JpaTest extends BaseTest {

    @Inject
    public JPAContext jpa;

    @Test
    public void init() {
        jpa.inTransaction(em -> {
            Message message = new Message();
            message.setTimestamp(System.currentTimeMillis());
            message.setUserId(1L);
            message.setMessage("message");
            em.persist(message);
        });

        jpa.managed(em -> {
            em.createQuery("select m from Message m", Message.class)
//                    .setParameter("message", "message")
                    .getResultStream()
                    .forEach(m -> log.info("{}", m));
        });
    }
}
