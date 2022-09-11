package ru.spliterash.pubSubMessaging.pubsub.binary.port;

/**
 * В байтах должно хранится, что это за объект, ну или какие-то сведения для десериализации
 */
public interface BinaryObjectMapper {
    byte[] write(Object obj) throws Exception;

    Object read(byte[] obj) throws Exception;
}
