package com.realtime.communication.chat.domain.model;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing a unique message identifier
 */
@Value
public class MessageId {
    UUID value;

    public static MessageId generate() {
        return new MessageId(UUID.randomUUID());
    }

    public static MessageId of(String value) {
        return new MessageId(UUID.fromString(value));
    }
}
