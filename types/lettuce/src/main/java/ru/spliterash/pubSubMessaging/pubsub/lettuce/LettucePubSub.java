package ru.spliterash.pubSubMessaging.pubsub.lettuce;

import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubGateway;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubListener;

@Slf4j
@RequiredArgsConstructor
public class LettucePubSub implements BinaryPubSubGateway {
    private final LettucePubSubConnectionProvider connectionProvider;
    private final LettuceConnectionExecutor connectionExecutor;

    @Override
    public Subscribe subscribe(String path, BinaryPubSubListener listener) {
        StatefulRedisPubSubConnection<String, byte[]> connection = connectionProvider.getConnection();

        connection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, byte[] message) {
                listener.onEvent(message);
            }
        });
        connection.sync().subscribe(path);
        log.info("Success subscribe on " + path);

        return connection::close;
    }

    @Override
    public void dispatch(String path, byte[] body) {
        connectionExecutor.execute(connection -> connection.publish(path, body));
    }
}
