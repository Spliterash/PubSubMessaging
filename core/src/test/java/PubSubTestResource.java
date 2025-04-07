import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface PubSubTestResource {
    /**
     * Просто умножить на 2
     */
    int multiply(int number);

    UUID noArgMethod();

    CompletableFuture<UUID> futureMethod();

    CompletableFuture<UUID> futureMethodExc();

    Future<UUID> notCompletableFutureMethodExc();

    Future<UUID> notCompletableFutureMethod();

    String expMethod(String str);

    void voidMethod(String str);

    CompletableFuture<Void> rlyLongTask();
}
