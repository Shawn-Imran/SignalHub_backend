package com.realtime.communication.auth.domain.model;

/**
 * Value object representing a User Profile
 */
public record UserProfile(String displayName, String avatarUrl, String bio) {
    public UserProfile {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }
        if (displayName.length() > 100) {
            throw new IllegalArgumentException("Display name cannot exceed 100 characters");
        }
    }
}

