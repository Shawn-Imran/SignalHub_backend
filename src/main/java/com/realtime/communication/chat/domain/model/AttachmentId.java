package com.realtime.communication.chat.domain.model;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing a unique attachment identifier
 */
@Value
public class AttachmentId {
    UUID value;

    public static AttachmentId generate() {
        return new AttachmentId(UUID.randomUUID());
    }

    public static AttachmentId of(String value) {
        return new AttachmentId(UUID.fromString(value));
    }
}
