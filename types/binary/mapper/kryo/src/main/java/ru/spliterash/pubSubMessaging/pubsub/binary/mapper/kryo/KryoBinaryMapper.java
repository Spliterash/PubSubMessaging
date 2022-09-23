package ru.spliterash.pubSubMessaging.pubsub.binary.mapper.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.Pool;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class KryoBinaryMapper implements BinaryObjectMapper {

    private final Pool<Kryo> kryoPool;
    private final Pool<Input> inputPool;
    private final Pool<Output> outputPool;
    private final KryoFactory factory;

    public KryoBinaryMapper(KryoFactory factory) {
        this.factory = factory;

        this.kryoPool = new Pool<Kryo>(true, false, 1024) {
            @Override
            protected Kryo create() {
                return createKryo();
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

        return kryo;
    }

    @Override
    public byte[] write(Object obj) {
        Kryo kryo = kryoPool.obtain();
        Output output = outputPool.obtain();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            output.setOutputStream(baos);
            kryo.writeClassAndObject(output, obj);
            output.flush();

            return baos.toByteArray();
        } finally {
            kryoPool.free(kryo);
            outputPool.free(output);
        }
    }

    @Override
    public Object read(byte[] obj) {
        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();
        try {
            input.setInputStream(new ByteArrayInputStream(obj));
            return kryo.readClassAndObject(input);
        } finally {
            kryoPool.free(kryo);
            inputPool.free(input);
        }
    }
}
