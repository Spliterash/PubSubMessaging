package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.kryo;

import com.esotericsoftware.kryo.Kryo;

public interface KryoFactory {
    Kryo createKryo();

    /**
     * Когда конфигурация поменяется, будет вызван
     */
    default void addOnChangeCallback(Runnable runnable){
        // Пусто, по дефолту
    }
}
