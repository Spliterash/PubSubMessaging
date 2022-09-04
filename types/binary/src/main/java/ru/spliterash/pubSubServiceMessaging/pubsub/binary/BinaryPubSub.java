package ru.spliterash.pubSubServiceMessaging.pubsub.binary;

import lombok.RequiredArgsConstructor;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.SubscribeListener;
import ru.spliterash.pubSubServiceMessaging.pubsub.binary.port.BinaryObjectMapper;
import ru.spliterash.pubSubServiceMessaging.pubsub.binary.port.BinaryPubSubGateway;

@RequiredArgsConstructor
public class BinaryPubSub implements PubSubGateway {
    private final BinaryObjectMapper binaryObjectMapper;
    private final BinaryPubSubGateway binaryPubSubGateway;

    @Override
    public <T> Subscribe subscribe(Class<T> topicClass, String path, SubscribeListener<T> listener) {
        return binaryPubSubGateway.subscribe(path, event -> {
            try {
                Object object = binaryObjectMapper.read(event);

                listener.onEventUnchecked(object);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    @Override
    public void dispatch(String path, Object event) {
        try {
            byte[] body = binaryObjectMapper.write(event);
            binaryPubSubGateway.dispatch(path, body);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
