package com.realtime.communication.auth.domain.model;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing a unique user identifier
 */
@Value
public class UserId {
    UUID value;

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }
}
