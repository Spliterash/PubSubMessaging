package ru.spliterash.pubSubMessaging.pubsub.jedis;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.UnifiedJedis;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubGateway;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubListener;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

@Log4j2
@RequiredArgsConstructor
public class JedisPubSub implements BinaryPubSubGateway {
    private final Executor executor;
    private final UnifiedJedis jedis;

    @Override
    public Subscribe subscribe(String path, BinaryPubSubListener listener) {
        BinaryJedisPubSub pubSub = new BinaryJedisPubSub() {
            @Override
            public void onMessage(byte[] channel, byte[] message) {
                listener.onEvent(message);
            }
        };

        executor.execute(() -> {
            log.info("Create Jedis Sub on path " + path);
            jedis.subscribe(pubSub, path.getBytes());
            log.info("Sub stop on path " + path);
        });

        return pubSub::unsubscribe;
    }

    @Override
    public void dispatch(String path, byte[] body) {
        jedis.publish(path.getBytes(StandardCharsets.UTF_8), body);
    }
}
