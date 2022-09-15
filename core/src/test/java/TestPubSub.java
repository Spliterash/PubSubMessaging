import lombok.Setter;
import ru.spliterash.pubSubMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubMessaging.base.pubSub.SubscribeListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestPubSub implements PubSubGateway {
    private final Map<String, Collection<SubscribeListener<?>>> listeners = new HashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    @Setter
    private volatile boolean needSleep = false;

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

            Object clonedObject = cloneObject(event);
            if (needSleep) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            for (SubscribeListener<?> subscribeListener : listener) {
                onEventUnchecked(subscribeListener, clonedObject);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void onEventUnchecked(SubscribeListener<?> listener, Object event) {
        ((SubscribeListener<T>) listener).onEvent((T) event);
    }

    private <T> T cloneObject(T object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (Exception exception) {
            executor.shutdown();
            throw new RuntimeException(exception);
        }
    }
}
