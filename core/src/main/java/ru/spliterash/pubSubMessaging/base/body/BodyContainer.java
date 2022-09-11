package ru.spliterash.pubSubMessaging.base.body;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@AllArgsConstructor
public final class BodyContainer<T> implements Serializable {
    private final UUID id;
    private final String senderDomain;
    private final String path;

    private final T request;
}
