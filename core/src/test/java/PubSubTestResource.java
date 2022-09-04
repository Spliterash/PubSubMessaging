import ru.spliterash.pubSubServiceMessaging.base.annotations.Request;

import java.util.UUID;

public interface PubSubTestResource {
    /**
     * Просто умножить на 2
     */
    @Request("test-request")
    int multiply(int number);

    @Request("no-arg-request")
    UUID noArgMethod();
}
