import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubServiceMessaging.base.service.PubSubServiceMessagingService;

public class PubSubServiceTest {
    private final PubSubGateway testPubSub = new TestPubSub();
    private final PubSubServiceMessagingService service1 = new PubSubServiceMessagingService(
            "",
            "service1",
            testPubSub
    );
    private final PubSubServiceMessagingService service2 = new PubSubServiceMessagingService(
            "",
            "service2",
            testPubSub
    );

    @Test
    public void testMessaging() {
        service2.registerHandler(PubSubTestResource.class, new PubSubTestController());

        PubSubTestResource client = service1.createClient("service2", PubSubTestResource.class);

        int number = 10;

        int result = client.multiply(number);

        Assertions.assertEquals(number * 2, result);
    }
}
