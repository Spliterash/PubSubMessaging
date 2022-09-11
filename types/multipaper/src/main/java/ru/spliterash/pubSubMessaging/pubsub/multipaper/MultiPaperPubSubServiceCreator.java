package ru.spliterash.pubSubMessaging.pubsub.multipaper;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.spliterash.pubSubMessaging.base.service.PubSubMessagingService;
import ru.spliterash.pubSubMessaging.pubsub.binary.BinaryPubSub;
import ru.spliterash.pubSubMessaging.pubsub.binary.mapper.serializable.JavaSerializableBinaryMapper;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryObjectMapper;
import ru.spliterash.pubSubMessaging.pubsub.multipaper.base.MultiPaperBinaryGateway;

@UtilityClass
public class MultiPaperPubSubServiceCreator {
    public static PubSubMessagingService createService(
            JavaPlugin plugin,
            String startPath,
            String domain,
            BinaryObjectMapper binaryObjectMapper
    ) {
        return new PubSubMessagingService(
                startPath,
                domain,
                new BinaryPubSub(binaryObjectMapper, new MultiPaperBinaryGateway(plugin))
        );
    }

    public static PubSubMessagingService createService(
            JavaPlugin plugin,
            String startPath,
            BinaryObjectMapper mapper
    ) {
        return createService(plugin, startPath, Bukkit.getServer().getLocalServerName(), mapper);
    }

    public static PubSubMessagingService createService(
            JavaPlugin plugin,
            String startPath
    ) {
        return createService(plugin, startPath, Bukkit.getServer().getLocalServerName(), new JavaSerializableBinaryMapper());
    }
}
