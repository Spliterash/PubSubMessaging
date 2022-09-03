package ru.spliterash.pubSubServiceMessaging.base.body;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public final class BodyContainer<T> {
    private final UUID id;
    private final String senderDomain;
    private final String path;

    private final T request;
}
