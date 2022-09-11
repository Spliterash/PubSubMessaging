package ru.spliterash.pubSubMessaging.pubsub.binary.port;

import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;

public interface BinaryPubSubGateway {
    Subscribe subscribe(String path, BinaryPubSubListener listener);

    void dispatch(String path, byte[] body);
}
