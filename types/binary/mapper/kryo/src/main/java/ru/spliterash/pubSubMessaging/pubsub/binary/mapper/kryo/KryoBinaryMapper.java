package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.Pool;
import lombok.AllArgsConstructor;
import org.objenesis.strategy.StdInstantiatorStrategy;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class KryoBinaryMapper implements BinaryObjectMapper {

    private final Pool<KryoContainer> kryoPool;
    private final Pool<Input> inputPool;
    private final Pool<Output> outputPool;
    private final KryoFactory factory;
    private volatile UUID poolStateUUID;

    public KryoBinaryMapper(KryoFactory factory) {
        this.factory = factory;
        factory.addOnChangeCallback(this::clearPools);

        this.kryoPool = new Pool<KryoContainer>(true, false, 1024) {
            @Override
            protected KryoContainer create() {
                return new KryoContainer(createKryo(), poolStateUUID);
            }
        };

        this.inputPool = new Pool<Input>(true, false, 512) {
            @Override
            protected Input create() {
                return new Input(8192);
            }
        };

        this.outputPool = new Pool<Output>(true, false, 512) {
            @Override
            protected Output create() {
                return new Output(8192, -1);
            }
        };
    }

    protected Kryo createKryo() {
        Kryo kryo = factory.createKryo();

        kryo.setRegistrationRequired(false);
        kryo.setReferences(false);
        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

        return kryo;
    }

    /**
     * Очистить "бассейны" (гыг), если изменилась конфигурация kryo например
     */
    public void clearPools() {
        poolStateUUID = UUID.randomUUID();
        kryoPool.clear();
    }

    private void free(KryoContainer kryo) {
        if (poolStateUUID.equals(kryo.state))
            kryoPool.free(kryo);
    }

    @Override
    public byte[] write(Object obj) {
        KryoContainer container = kryoPool.obtain();
        Output output = outputPool.obtain();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            output.setOutputStream(baos);
            container.kryo.writeClassAndObject(output, obj);
            output.flush();

            return baos.toByteArray();
        } finally {
            free(container);
            outputPool.free(output);
        }
    }

    @Override
    public Object read(byte[] obj) {
        KryoContainer container = kryoPool.obtain();
        Input input = inputPool.obtain();
        try {
            input.setInputStream(new ByteArrayInputStream(obj));
            return container.kryo.readClassAndObject(input);
        } finally {
            free(container);
            inputPool.free(input);
        }
    }

    @AllArgsConstructor
    private static class KryoContainer {
        private final Kryo kryo;
        private final UUID state;
    }
}
