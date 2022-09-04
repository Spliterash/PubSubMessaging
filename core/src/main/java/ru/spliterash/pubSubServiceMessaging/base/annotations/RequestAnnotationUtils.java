package ru.spliterash.pubSubServiceMessaging.base.annotations;

public class RequestAnnotationUtils {
    public static String getFullPath(Class<?> resourceClass, Request request) {
        return resourceClass.getSimpleName() + ":" + request.value();
    }
}
