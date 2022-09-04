package ru.spliterash.pubSubServiceMessaging.pubsub.multipaper;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.spliterash.pubSubServiceMessaging.base.service.PubSubServiceMessagingService;
import ru.spliterash.pubSubServiceMessaging.pubsub.binary.BinaryPubSub;
import ru.spliterash.pubSubServiceMessaging.pubsub.binary.mapper.serializable.JavaSerializableBinaryMapper;
import ru.spliterash.pubSubServiceMessaging.pubsub.binary.port.BinaryObjectMapper;
import ru.spliterash.pubSubServiceMessaging.pubsub.multipaper.base.MultiPaperBinaryGateway;

@UtilityClass
public class MultiPaperPubSubServiceCreator {
    public static PubSubServiceMessagingService createService(
            JavaPlugin plugin,
            String startPath,
            String domain,
            BinaryObjectMapper binaryObjectMapper
    ) {
        return new PubSubServiceMessagingService(
                startPath,
                domain,
                new BinaryPubSub(binaryObjectMapper, new MultiPaperBinaryGateway(plugin))
        );
    }

    public static PubSubServiceMessagingService createService(
            JavaPlugin plugin,
            String startPath,
            BinaryObjectMapper mapper
    ) {
        return createService(plugin, startPath, Bukkit.getServer().getLocalServerName(), mapper);
    }

    public static PubSubServiceMessagingService createService(
            JavaPlugin plugin,
            String startPath
    ) {
        return createService(plugin, startPath, Bukkit.getServer().getLocalServerName(), new JavaSerializableBinaryMapper());
    }
}
