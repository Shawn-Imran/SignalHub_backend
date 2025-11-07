package com.realtime.communication.auth.domain.model;

import lombok.Value;

import java.util.Objects;

/**
 * Value object representing a username
 */
@Value
public class Username {
    String value;

    public Username(String value) {
        this.value = validate(value);
    }

    private String validate(String value) {
        Objects.requireNonNull(value, "Username cannot be null");
        if (value.length() < 3 || value.length() > 30) {
            throw new IllegalArgumentException("Username must be between 3 and 30 characters");
        }
        if (!value.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
        return value;
    }
}

