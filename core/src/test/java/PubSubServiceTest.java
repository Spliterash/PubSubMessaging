import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.spliterash.pubSubMessaging.base.exceptions.PubSubTimeout;
import ru.spliterash.pubSubMessaging.base.service.PubSubMessagingService;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PubSubServiceTest {
    private static final TestPubSub testPubSub = new TestPubSub();
    private static final Consumer<String> consumer = Mockito.mock(Consumer.class);
    private static final PubSubMessagingService service1 = new PubSubMessagingService(
            "",
            "service1",
            testPubSub
    );
    private static final PubSubMessagingService service2 = new PubSubMessagingService(
            "",
            "service2",
            testPubSub
    );
    private static PubSubTestResource service2Client;
    private static PubSubTestResource unknownClient;

    @BeforeAll
    public static void init() {
        service2.registerHandler(PubSubTestResource.class, new PubSubTestController(consumer));
        service2Client = service1.createClient("service2", PubSubTestResource.class);
        unknownClient = service1.createClient("unknown", PubSubTestResource.class);
    }

    @AfterAll
    public static void destroy() {
        service1.destroy();
        service2.destroy();
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
    public void testException() {
        String str = "12345";
        try {
            service2Client.expMethod(str);
            fail("Exception not throws");
        } catch (RuntimeException exception) {
            Assertions.assertEquals(str, exception.getMessage());
        }
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
        try {
            service2Client.futureMethodExc().whenComplete((uuid, throwable) -> {
                called.run();
                Assertions.assertNull(uuid);
                Assertions.assertNotNull(throwable);
                Assertions.assertInstanceOf(TestException.class, throwable);
            }).join();
        } catch (CompletionException ignore) {
        }
        Mockito.verify(called).run();
    }

    @Test
    public void testNotCompletableFutureException() {
        try {
            service2Client.notCompletableFutureMethodExc().get();
            fail("Exception not throws");
        } catch (InterruptedException | ExecutionException e) {
            Assertions.assertInstanceOf(TestException.class, e.getCause());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void optimizedVoidCallsTest() throws IllegalAccessException, NoSuchFieldException, InterruptedException {
        testPubSub.setNeedSleep(true);
        String testStr = "someString";

        Field field = service2.getClass().getDeclaredField("outboundRequestsInProcess");
        field.setAccessible(true);

        Map requestsInProcess = (Map) field.get(service2);

        Assertions.assertEquals(0, requestsInProcess.size());
        Assertions.assertTimeoutPreemptively(Duration.ofMillis(200), () -> service2Client.voidMethod(testStr));
        Assertions.assertEquals(0, requestsInProcess.size());

        Thread.sleep(1100);

        Mockito.verify(consumer).accept(testStr);

        testPubSub.setNeedSleep(false);
    }

    @Test
    public void timeoutReply() throws InterruptedException {
        long start = System.currentTimeMillis();
        service2Client
                .rlyLongTask()
                .join();
        long timeSpend = System.currentTimeMillis() - start;

        Assertions.assertTrue(timeSpend >= 10000 && timeSpend <= 15000);
    }

    @Test
    public void timeoutReplyMulti() throws InterruptedException {
        long start = System.currentTimeMillis();
        var task1 = service2Client
                .rlyLongTask();
        var task2 = service2Client
                .rlyLongTask();
        var task3 = service2Client
                .rlyLongTask();
        task1.join();
        task2.join();
        task3.join();

        long timeSpend = System.currentTimeMillis() - start;
        System.out.println(timeSpend);
        Assertions.assertTrue(10000 <= timeSpend && timeSpend <= 15000);
    }

    @Test
    public void noHeartbeat() {
        try {
            unknownClient.futureMethod().join();
        } catch (CompletionException exp) {
            assertEquals(PubSubTimeout.class, exp.getCause().getClass());
            return;
        }

        fail("Exception not thrown");
    }
}
