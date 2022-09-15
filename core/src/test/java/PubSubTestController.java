import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class PubSubTestController implements PubSubTestResource {
    private final Consumer<String> consumer;

    @Override
    public int multiply(int number) {
        return number * 2;
    }

    @Override
    public UUID noArgMethod() {
        return TestData.randomUUID;
    }

    @Override
    public CompletableFuture<UUID> futureMethod() {
        return CompletableFuture.completedFuture(TestData.randomUUID);
    }

    @Override
    public CompletableFuture<UUID> futureMethodExc() {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        new Thread(() -> future.completeExceptionally(new TestException())).start();

        return future;
    }

    @Override
    public Future<UUID> notCompletableFutureMethodExc() {
        return new Future<UUID>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public UUID get() throws InterruptedException, ExecutionException {
                throw new ExecutionException(new TestException());
            }

            @Override
            public UUID get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return get();
            }
        };
    }

    @Override
    public Future<UUID> notCompletableFutureMethod() {
        return new Future<UUID>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public UUID get() throws InterruptedException, ExecutionException {
                return TestData.randomUUID;
            }

            @Override
            public UUID get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return get();
            }
        };
    }

    @Override
    public void voidMethod(String str) {
        consumer.accept(str);
    }
}
