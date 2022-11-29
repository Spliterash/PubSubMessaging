package ru.spliterash.pubSubMessaging.pubsub.lettuce;

import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubGateway;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubListener;

@Log4j2
@RequiredArgsConstructor
public class LettucePubSub implements BinaryPubSubGateway {
    private final LettuceConnectionProvider connectionProvider;

    @Override
    public Subscribe subscribe(String path, BinaryPubSubListener listener) {
        StatefulRedisPubSubConnection<String, byte[]> connection = connectionProvider.getConnection();

        connection.addListener(new RedisPubSubAdapter<String, byte[]>() {
            @Override
            public void message(String channel, byte[] message) {
                listener.onEvent(message);
            }
        });
        log.info("Try sub on " + path);
        connection.sync().subscribe(path);
        log.info("Success subscribe on " + path);


        return () -> {
            connection.sync().unsubscribe(path);
            connection.close();
        };
    }

    @Override
    public void dispatch(String path, byte[] body) {
        try (StatefulRedisPubSubConnection<String, byte[]> connection = connectionProvider.getConnection()) {
            connection.sync().publish(path, body);
        }
    }
}
