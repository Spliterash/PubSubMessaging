package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.kryo;

import com.esotericsoftware.kryo.Kryo;

public interface KryoFactory {
    Kryo createKryo();
}
