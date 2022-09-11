package ru.spliterash.pubSubMessaging.pubsub.multipaper.base;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubGateway;
import ru.spliterash.pubSubMessaging.pubsub.binary.port.BinaryPubSubListener;

@RequiredArgsConstructor
public class MultiPaperBinaryGateway implements BinaryPubSubGateway {
    private final JavaPlugin plugin;

    @Override
    public Subscribe subscribe(String path, BinaryPubSubListener listener) {
        Bukkit.getMultiPaperNotificationManager().on(plugin, path, listener::onEvent);

        return () -> {
            // Not implemented in multipaper;
        };
    }

    @Override
    public void dispatch(String path, byte[] body) {
        Bukkit.getMultiPaperNotificationManager().notify(path, body);
    }
}
