package ru.spliterash.pubSubServiceMessaging.base.pubSub;

@FunctionalInterface
public interface SubscribeListener<T> {
    void onEvent(T event);

    @SuppressWarnings("unchecked")
    default void onEventUnchecked(Object object) {
        onEvent((T) object);
    }
}
