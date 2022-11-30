package ru.spliterash.pubSubMessaging.pubsub.lettuce;

import io.lettuce.core.api.sync.BaseRedisCommands;

import java.util.function.Consumer;

public interface LettuceConnectionExecutor {
    void execute(Consumer<BaseRedisCommands<String, byte[]>> consumer);
}
