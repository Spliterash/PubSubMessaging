package ru.spliterash.pubSubMessaging.pubsub.lettuce;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

public interface LettucePubSubConnectionProvider {
    StatefulRedisPubSubConnection<String, byte[]> getConnection();
}