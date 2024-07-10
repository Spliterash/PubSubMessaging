package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryObjectMapper;

import java.util.List;

public class JacksonMapper implements BinaryObjectMapper {
    private final ObjectMapper mapper;

    public JacksonMapper(List<Module> jacksonModules) {
        mapper = new ObjectMapper();
        mapper.registerModules(jacksonModules);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.registerModules(new SimpleModule()
                .addSerializer(Throwable.class, new ThrowableSerializer())
                .addDeserializer(Throwable.class, new ThrowableDeserializer())
        );


        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
    }


    @Override
    public byte[] write(Object obj) throws Exception {
        return mapper.writeValueAsBytes(obj);
    }

    @Override
    public Object read(byte[] obj) throws Exception {
        return mapper.readValue(obj, Object.class);
    }
}
