package ru.spliterash.pubSubServiceMessaging.base.pubSub;

public interface PubSubGateway {
    <T> Subscribe subscribe(Class<T> topicClass, String path, SubscribeListener<T> listener);

    void dispatch(String path, Object event);

    default String getNamespaceDelimiter() {
        return ":";
    }
}
