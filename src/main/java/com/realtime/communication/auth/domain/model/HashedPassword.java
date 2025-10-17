package com.realtime.communication.auth.domain.model;

import com.realtime.communication.shared.domain.exception.ValidationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Value object representing a hashed password
 */
public class HashedPassword {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final String hash;

    private HashedPassword(String hash) {
        this.hash = hash;
    }

    public static HashedPassword fromPlainText(String plainPassword) {
        validatePassword(plainPassword);
        return new HashedPassword(encoder.encode(plainPassword));
    }

    public static HashedPassword fromHash(String hash) {
        return new HashedPassword(hash);
    }

    public boolean matches(String plainPassword) {
        return encoder.matches(plainPassword, hash);
    }

    public String getHash() {
        return hash;
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password cannot be empty");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        // Additional password strength validation can be added here
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashedPassword that = (HashedPassword) o;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }
}

