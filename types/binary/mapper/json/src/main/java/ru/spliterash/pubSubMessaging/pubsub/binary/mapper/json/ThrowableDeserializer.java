package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Objects;

public class ThrowableDeserializer extends StdDeserializer<Throwable> {

    public ThrowableDeserializer() {
        super(Throwable.class);
    }

    @Override
    public Throwable deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
        String content = null;
        String lastValue;
        do {
            lastValue = p.getValueAsString();
            p.nextToken();
            if (Objects.equals(lastValue, "content")) {
                content = p.getValueAsString();
                break;
            }
        } while (lastValue != null);
        p.nextToken();
        String text = p.getValueAsString();
        byte[] data = Base64.getDecoder().decode(content);

        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
        try {
            return (Throwable) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found during deserialization", e);
        }
    }
}