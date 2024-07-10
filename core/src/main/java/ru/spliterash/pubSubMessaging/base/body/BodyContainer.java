package ru.spliterash.pubSubMessaging.base.body;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Jacksonized
@Builder
@AllArgsConstructor
public final class BodyContainer<T> implements Serializable {
    private final UUID id;
    private final String senderDomain;
    private final String path;

    private final T request;
}
