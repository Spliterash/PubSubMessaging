import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubServiceMessaging.base.service.PubSubServiceMessagingService;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PubSubServiceTest {
    private static final PubSubGateway testPubSub = new TestPubSub();
    private static final PubSubServiceMessagingService service1 = new PubSubServiceMessagingService(
            "",
            "service1",
            testPubSub
    );
    private static final PubSubServiceMessagingService service2 = new PubSubServiceMessagingService(
            "",
            "service2",
            testPubSub
    );
    private static PubSubTestResource service2Client;

    @BeforeAll
    public static void init() {
        service2.registerHandler(PubSubTestResource.class, new PubSubTestController());
        service2Client = service1.createClient("service2", PubSubTestResource.class);
    }

    @Test
    public void testSimpleMessaging() {
        int number = 10;

        int result = service2Client.multiply(number);

        Assertions.assertEquals(number * 2, result);

        UUID uuid = service2Client.noArgMethod();

        Assertions.assertEquals(TestData.randomUUID, uuid);
    }

    @Test
    public void testFutureMessaging() throws Exception {
        Object uuid = service2Client.futureMethod().get();

        Assertions.assertEquals(TestData.randomUUID, uuid);
    }

    @Test
    public void testNotCompletableFutureMessaging() throws Exception {
        Object uuid = service2Client.notCompletableFutureMethod().get();

        Assertions.assertEquals(TestData.randomUUID, uuid);
    }

    @Test
    public void testFutureException() {
        Runnable called = Mockito.mock(Runnable.class);
        service2Client.futureMethodExc().whenComplete((uuid, throwable) -> {
            called.run();
            Assertions.assertNull(uuid);
            Assertions.assertNotNull(throwable);
            Assertions.assertInstanceOf(TestException.class, throwable);
        });

        Mockito.verify(called).run();
    }

    @Test
    public void testNotCompletableFutureException() {
        try {
            service2Client.notCompletableFutureMethodExc().get();
            Assertions.fail("Exception not throws");
        } catch (InterruptedException | ExecutionException e) {
            Assertions.assertInstanceOf(TestException.class, e.getCause());
        }
    }


}
