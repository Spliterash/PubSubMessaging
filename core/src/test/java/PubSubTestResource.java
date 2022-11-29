import ru.spliterash.pubSubMessaging.base.annotations.Request;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface PubSubTestResource {
    /**
     * Просто умножить на 2
     */
    @Request("test-request")
    int multiply(int number);

    @Request("no-arg-request")
    UUID noArgMethod();

    @Request("future-method")
    CompletableFuture<UUID> futureMethod();

    @Request("future-method-exp")
    CompletableFuture<UUID> futureMethodExc();

    @Request("not-completable-future-method-exp")
    Future<UUID> notCompletableFutureMethodExc();

    @Request("not-completable-future-method")
    Future<UUID> notCompletableFutureMethod();

    @Request("exp-method")
    String expMethod(String str);

    @Request("void-method")
    void voidMethod(String str);

    /**
     * Метод который никогда не выполнится
     */
    @Request("never-complete-future")
    CompletableFuture<Void> neverCompleteFuture();
}
