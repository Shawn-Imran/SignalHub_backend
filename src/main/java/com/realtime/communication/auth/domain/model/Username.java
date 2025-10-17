package com.realtime.communication.auth.domain.model;

import com.realtime.communication.shared.domain.exception.ValidationException;

/**
 * Value object representing a Username
 * Validation: 3-30 characters, alphanumeric and underscore only
 */
public record Username(String value) {
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,30}$";

    public Username {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (value.length() < 3 || value.length() > 30) {
            throw new ValidationException("Username must be between 3 and 30 characters");
        }
        if (!value.matches(USERNAME_PATTERN)) {
            throw new ValidationException("Username can only contain letters, numbers, and underscores");
        }
    }
}

