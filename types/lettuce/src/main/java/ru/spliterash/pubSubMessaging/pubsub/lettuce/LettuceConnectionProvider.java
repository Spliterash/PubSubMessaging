package ru.spliterash.pubSubMessaging.pubsub.lettuce;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

public interface LettuceConnectionProvider {
    StatefulRedisPubSubConnection<String, byte[]> getConnection();
}