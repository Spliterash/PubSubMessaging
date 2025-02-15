package ru.spliterash.pubSubMessaging.base.annotations;

import java.lang.reflect.Method;

public class RequestAnnotationUtils {
    public static String getFullPath(Class<?> resourceClass, Method method, Request request) {
        String name;
        if (request != null)
            name = request.value();
        else
            name = method.getName();
        return resourceClass.getSimpleName() + ":" + name;
    }
}
