package com.realtime.communication.auth.domain.model;

import com.realtime.communication.shared.domain.model.BaseEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * User Aggregate Root
 * Represents a user in the authentication context
 */
public class User extends BaseEntity {
    private final UserId id;
    private Username username;
    private Email email;
    private HashedPassword password;
    private UserProfile profile;
    private UserStatus status;
    private Set<Role> roles;
    private boolean blocked;
    private boolean emailVerified;
    private Instant lastSeenAt;

    public User(UserId id, Username username, Email email, HashedPassword password, UserProfile profile) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.profile = profile;
        this.status = UserStatus.OFFLINE;
        this.roles = new HashSet<>();
        this.blocked = false;
        this.emailVerified = false;
        this.lastSeenAt = null;
    }

    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
        if (newStatus == UserStatus.OFFLINE) {
            this.lastSeenAt = Instant.now();
        }
    }

    public void block() {
        this.blocked = true;
    }

    public void unblock() {
        this.blocked = false;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void updateProfile(UserProfile newProfile) {
        this.profile = newProfile;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    // Getters
    public UserId getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public HashedPassword getPassword() {
        return password;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Set<Role> getRoles() {
        return new HashSet<>(roles);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}

