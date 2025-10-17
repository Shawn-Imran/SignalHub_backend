package com.realtime.communication.auth.domain.model;

import com.realtime.communication.shared.domain.exception.ValidationException;

/**
 * Value object representing an Email address
 */
public record Email(String value) {
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Email cannot be empty");
        }
        if (!value.matches(EMAIL_PATTERN)) {
            throw new ValidationException("Invalid email format");
        }
    }
}

