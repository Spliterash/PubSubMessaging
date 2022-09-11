package ru.spliterash.pubSubMessaging.base.body;

import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Getter
public final class RequestCompleteContainer<T> implements Serializable {
    private final UUID requestID;

    public RequestCompleteContainer(UUID requestID, T response) {
        this.requestID = requestID;
        this.response = response;
        this.exception = null;
    }

    public RequestCompleteContainer(UUID requestID, Throwable exception) {
        this.requestID = requestID;
        this.response = null;
        this.exception = exception;
    }

    private final Throwable exception;
    private final T response;

    public boolean isSuccess() {
        return exception == null;
    }
}
