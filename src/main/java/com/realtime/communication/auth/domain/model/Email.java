package com.realtime.communication.auth.domain.model;

import lombok.Value;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing an email address
 */
@Value
public class Email {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    String value;

    public Email(String value) {
        this.value = validate(value);
    }

    private String validate(String value) {
        Objects.requireNonNull(value, "Email cannot be null");
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return value.toLowerCase();
    }
}

