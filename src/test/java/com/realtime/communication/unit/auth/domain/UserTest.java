package com.realtime.communication.unit.auth.domain;

import com.realtime.communication.auth.domain.model.*;
import com.realtime.communication.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User entity (Domain Layer)
 * TDD: These tests are written BEFORE implementation
 */
@DisplayName("User Entity Tests")
class UserTest {

    @Test
    @DisplayName("Should create valid user with all required fields")
    void shouldCreateValidUser() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        Username username = new Username("testuser");
        Email email = new Email("test@example.com");
        HashedPassword password = HashedPassword.fromPlainText("SecurePass123!");
        UserProfile profile = new UserProfile("Test User", null, null);

        // When
        User user = new User(userId, username, email, password, profile);

        // Then
        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(UserStatus.OFFLINE, user.getStatus());
        assertFalse(user.isBlocked());
        assertFalse(user.isEmailVerified());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Should throw exception when username is invalid")
    void shouldThrowExceptionForInvalidUsername() {
        // When/Then
        assertThrows(ValidationException.class, () -> new Username("ab")); // Too short
        assertThrows(ValidationException.class, () -> new Username("a".repeat(31))); // Too long
        assertThrows(ValidationException.class, () -> new Username("user@name")); // Invalid chars
    }

    @Test
    @DisplayName("Should throw exception when email is invalid")
    void shouldThrowExceptionForInvalidEmail() {
        // When/Then
        assertThrows(ValidationException.class, () -> new Email("invalid-email"));
        assertThrows(ValidationException.class, () -> new Email("@example.com"));
        assertThrows(ValidationException.class, () -> new Email("user@"));
    }

    @Test
    @DisplayName("Should update user status")
    void shouldUpdateUserStatus() {
        // Given
        User user = createValidUser();

        // When
        user.updateStatus(UserStatus.ONLINE);

        // Then
        assertEquals(UserStatus.ONLINE, user.getStatus());
        assertNotNull(user.getLastSeenAt());
    }

    @Test
    @DisplayName("Should block and unblock user")
    void shouldBlockAndUnblockUser() {
        // Given
        User user = createValidUser();

        // When
        user.block();

        // Then
        assertTrue(user.isBlocked());

        // When
        user.unblock();

        // Then
        assertFalse(user.isBlocked());
    }

    @Test
    @DisplayName("Should verify email")
    void shouldVerifyEmail() {
        // Given
        User user = createValidUser();
        assertFalse(user.isEmailVerified());

        // When
        user.verifyEmail();

        // Then
        assertTrue(user.isEmailVerified());
    }

    @Test
    @DisplayName("Should update user profile")
    void shouldUpdateUserProfile() {
        // Given
        User user = createValidUser();
        UserProfile newProfile = new UserProfile("New Name", "http://avatar.url", "New bio");

        // When
        user.updateProfile(newProfile);

        // Then
        assertEquals(newProfile, user.getProfile());
    }

    @Test
    @DisplayName("Should track last seen timestamp when going offline")
    void shouldTrackLastSeenWhenGoingOffline() {
        // Given
        User user = createValidUser();
        Instant beforeUpdate = Instant.now();

        // When
        user.updateStatus(UserStatus.OFFLINE);

        // Then
        assertNotNull(user.getLastSeenAt());
        assertTrue(user.getLastSeenAt().isAfter(beforeUpdate) || user.getLastSeenAt().equals(beforeUpdate));
    }

    private User createValidUser() {
        return new User(
            new UserId(UUID.randomUUID()),
            new Username("testuser"),
            new Email("test@example.com"),
            HashedPassword.fromPlainText("SecurePass123!"),
            new UserProfile("Test User", null, null)
        );
    }
}

