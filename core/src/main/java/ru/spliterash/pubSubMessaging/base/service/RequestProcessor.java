package ru.spliterash.pubSubMessaging.base.service;

public interface RequestProcessor<I, O> {
    O onRequest(I input) throws Throwable;

    default Object onRequestUnchecked(Object in) throws Throwable {
        //noinspection unchecked
        return onRequest((I) in);
    }
}
