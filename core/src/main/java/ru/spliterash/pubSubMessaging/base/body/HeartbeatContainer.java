package ru.spliterash.pubSubMessaging.base.body;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class HeartbeatContainer implements Serializable {
    private final UUID requestID;
}
