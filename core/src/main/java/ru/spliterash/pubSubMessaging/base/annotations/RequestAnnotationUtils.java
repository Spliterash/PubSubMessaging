package ru.spliterash.pubSubMessaging.base.annotations;

public class RequestAnnotationUtils {
    public static String getFullPath(Class<?> resourceClass, Request request) {
        return resourceClass.getSimpleName() + ":" + request.value();
    }
}
