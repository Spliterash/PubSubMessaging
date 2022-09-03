package ru.spliterash.pubSubServiceMessaging.base.body;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public final class RequestCompleteContainer<T> {
    private final UUID requestID;
    private final T response;
}
