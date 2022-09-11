package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.serializable;

import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryObjectMapper;

import java.io.*;

public class JavaSerializableBinaryMapper implements BinaryObjectMapper {
    @Override
    public byte[] write(Object obj) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ObjectOutputStream stream = new ObjectOutputStream(outputStream)) {
            stream.writeObject(obj);
        }

        return outputStream.toByteArray();
    }

    @Override
    public Object read(byte[] obj) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(obj);
        try (ObjectInputStream stream = new ObjectInputStream(inputStream)) {
            return stream.readObject();
        }
    }
}
