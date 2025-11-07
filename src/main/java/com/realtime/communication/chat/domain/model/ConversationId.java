package com.realtime.communication.chat.domain.model;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing a unique conversation identifier
 */
@Value
public class ConversationId {
    UUID value;

    public static ConversationId generate() {
        return new ConversationId(UUID.randomUUID());
    }

    public static ConversationId of(String value) {
        return new ConversationId(UUID.fromString(value));
    }
}
