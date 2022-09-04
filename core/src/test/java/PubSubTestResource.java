import ru.spliterash.pubSubServiceMessaging.base.annotations.Request;

public interface PubSubTestResource {
    /**
     * Просто умножить на 2
     */
    @Request("test-request")
    int multiply(int number);
}
