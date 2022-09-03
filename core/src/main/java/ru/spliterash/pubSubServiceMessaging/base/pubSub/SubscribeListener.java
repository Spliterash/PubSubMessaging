package ru.spliterash.pubSubServiceMessaging.base.pubSub;

@FunctionalInterface
public interface SubscribeListener<T> {
    void onEvent(T event);
}
