import ru.spliterash.pubSubServiceMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.SubscribeListener;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestPubSub implements PubSubGateway {
    private final Map<String, Collection<SubscribeListener<?>>> listeners = new HashMap<>();
    private final Executor executor = Executors.newCachedThreadPool();

    @Override
    public <T> Subscribe subscribe(Class<T> topicClass, String path, SubscribeListener<T> listener) {
        Collection<SubscribeListener<?>> list = listeners.computeIfAbsent(path, a -> new ArrayList<>());
        list.add(listener);

        return () -> list.remove(listener);
    }

    @Override
    public void dispatch(String path, Object event) {
        executor.execute(() -> {
            Collection<SubscribeListener<?>> listener = listeners.getOrDefault(path, Collections.emptyList());

            for (SubscribeListener<?> subscribeListener : listener) {
                onEventUnchecked(subscribeListener, event);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void onEventUnchecked(SubscribeListener<?> listener, Object event) {
        ((SubscribeListener<T>) listener).onEvent((T) event);
    }
}
