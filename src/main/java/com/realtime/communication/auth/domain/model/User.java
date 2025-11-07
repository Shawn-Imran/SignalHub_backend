package com.realtime.communication.auth.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * User aggregate root
 */
@Getter
public class User {
    private final UserId id;
    private final Username username;
    private final Email email;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private UserStatus status;
    private boolean blocked;
    private boolean emailVerified;
    private final Instant createdAt;
    private Instant lastSeenAt;

    // Constructor for creating a new user
    public User(UserId id, Username username, Email email, String passwordHash) {
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
        this.status = UserStatus.OFFLINE;
        this.blocked = false;
        this.emailVerified = false;
        this.createdAt = Instant.now();
    }

    // Full constructor for reconstitution from persistence
    public User(UserId id, Username username, Email email, String passwordHash,
                String displayName, String avatarUrl, String bio, UserStatus status,
                boolean blocked, boolean emailVerified, Instant createdAt, Instant lastSeenAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.status = status;
        this.blocked = blocked;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
    }

    // Domain methods
    public void updateStatus(UserStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "Status cannot be null");
        if (newStatus == UserStatus.ONLINE || newStatus == UserStatus.AWAY || newStatus == UserStatus.BUSY) {
            this.lastSeenAt = Instant.now();
        }
    }

    public void updateProfile(String displayName, String avatarUrl, String bio) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "Password hash cannot be null");
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void block() {
        this.blocked = true;
        this.status = UserStatus.OFFLINE;
    }

    public void unblock() {
        this.blocked = false;
    }

    public boolean isActive() {
        return !blocked && emailVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username=" + username +
                ", email=" + email +
                ", status=" + status +
                ", blocked=" + blocked +
                '}';
    }
}

