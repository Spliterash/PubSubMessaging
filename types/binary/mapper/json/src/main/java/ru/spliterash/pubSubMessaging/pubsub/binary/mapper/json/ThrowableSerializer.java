package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ThrowableSerializer extends JsonSerializer<Throwable> {

    @Override
    public void serialize(Throwable value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(value);
        objectOutputStream.flush();
        objectOutputStream.close();

        gen.writeBinary(byteArrayOutputStream.toByteArray());
    }

    @Override
    public void serializeWithType(Throwable value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        gen.writeStartObject();

        String property = typeSer.getPropertyName();
        gen.writeStringField(property, Throwable.class.getName());
        gen.writeStringField("simpleType", value.getClass().getSimpleName());
        gen.writeFieldName("content");
        serialize(value, gen, serializers);

        gen.writeEndObject();
    }
}

