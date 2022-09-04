package ru.spliterash.pubSubServiceMessaging.pubsub.binary.port;

import ru.spliterash.pubSubServiceMessaging.base.pubSub.Subscribe;

public interface BinaryPubSubGateway {
    Subscribe subscribe(String path, BinaryPubSubListener listener);

    void dispatch(String path, byte[] body);
}
