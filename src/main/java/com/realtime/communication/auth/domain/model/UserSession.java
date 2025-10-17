package com.realtime.communication.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * UserSession entity
 * Represents an active user session with tokens
 */
public class UserSession {
    private final UUID id;
    private final UserId userId;
    private String accessToken;
    private String refreshToken;
    private String deviceInfo;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;

    public UserSession(UUID id, UserId userId, String accessToken, String refreshToken,
                      String deviceInfo, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.deviceInfo = deviceInfo;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}

