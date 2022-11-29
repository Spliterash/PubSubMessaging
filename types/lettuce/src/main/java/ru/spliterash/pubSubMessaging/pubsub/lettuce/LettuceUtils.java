package ru.spliterash.pubSubMessaging.pubsub.lettuce;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

public class LettuceUtils {
    public static RedisCodec<String, byte[]> getBinaryCodec() {
        return RedisCodec.of(
                StringCodec.UTF8,
                new ByteArrayCodec()
        );
    }
}
