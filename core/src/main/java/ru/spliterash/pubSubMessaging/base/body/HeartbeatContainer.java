package ru.spliterash.pubSubMessaging.base.body;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Jacksonized
@Builder
@RequiredArgsConstructor
public class HeartbeatContainer implements Serializable {
    private final UUID requestID;
}
