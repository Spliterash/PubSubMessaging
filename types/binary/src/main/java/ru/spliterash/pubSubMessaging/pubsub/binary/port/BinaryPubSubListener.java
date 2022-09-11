package ru.spliterash.pubSubMessaging.pubsub.binary.port;

public interface BinaryPubSubListener {
    void onEvent(byte[] event);
}
