package ru.spliterash.pubSubServiceMessaging.pubsub.binary.port;

public interface BinaryPubSubListener {
    void onEvent(byte[] event);
}
