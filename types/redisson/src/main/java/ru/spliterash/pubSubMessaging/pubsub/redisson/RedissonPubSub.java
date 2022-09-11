package ru.spliterash.pubSubMessaging.pubsub.redisson;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import ru.spliterash.pubSubMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubMessaging.base.pubSub.SubscribeListener;

@RequiredArgsConstructor
public class RedissonPubSub implements PubSubGateway {
    private final RedissonClient client;

    @Override
    public <T> Subscribe subscribe(Class<T> topicClass, String path, SubscribeListener<T> listener) {
        RTopic topic = client.getTopic(path);

        int listenerId = topic.addListener(Object.class, (channel, msg) -> listener.onEventUnchecked(msg));

        return () -> topic.removeListener(listenerId);
    }

    @Override
    public void dispatch(String path, Object event) {
        client.getTopic(path).publishAsync(event);
    }
}
