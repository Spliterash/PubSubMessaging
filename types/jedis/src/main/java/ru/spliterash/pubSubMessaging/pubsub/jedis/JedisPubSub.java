package ru.spliterash.pubSubMessaging.pubsub.jedis;

import lombok.RequiredArgsConstructor;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.UnifiedJedis;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubGateway;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubListener;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class JedisPubSub implements BinaryPubSubGateway {
    private final UnifiedJedis jedis;

    @Override
    public Subscribe subscribe(String path, BinaryPubSubListener listener) {
        BinaryJedisPubSub pubSub = new BinaryJedisPubSub() {
            @Override
            public void onMessage(byte[] channel, byte[] message) {
                listener.onEvent(message);
            }
        };

        jedis.subscribe(pubSub);

        return pubSub::unsubscribe;
    }

    @Override
    public void dispatch(String path, byte[] body) {
        jedis.publish(path.getBytes(StandardCharsets.UTF_8), body);
    }
}
