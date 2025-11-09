package com.realtime.communication.unit.auth.domain;

import com.realtime.communication.auth.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User entity
 * Following TDD: These tests verify User domain logic
 */
@DisplayName("User Entity Tests")
class UserTest {

    private UserId userId;
    private Username username;
    private Email email;
    private String passwordHash;

    @BeforeEach
    void setUp() {
        userId = new UserId(UUID.randomUUID());
        username = new Username("testuser");
        email = new Email("test@example.com");
        passwordHash = "$2a$10$hashedpassword";
    }

    @Nested
    @DisplayName("User Creation Tests")
    class UserCreationTests {

        @Test
        @DisplayName("Should create user with valid parameters")
        void shouldCreateUserWithValidParameters() {
            // When
            User user = new User(userId, username, email, passwordHash);

            // Then
            assertNotNull(user);
            assertEquals(userId, user.getId());
            assertEquals(username, user.getUsername());
            assertEquals(email, user.getEmail());
            assertEquals(passwordHash, user.getPasswordHash());
            assertEquals(UserStatus.OFFLINE, user.getStatus());
            assertFalse(user.isBlocked());
            assertFalse(user.isEmailVerified());
            assertNotNull(user.getCreatedAt());
            assertFalse(user.isActive()); // Not active because email not verified
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new User(null, username, email, passwordHash)
            );
        }

        @Test
        @DisplayName("Should throw exception when username is null")
        void shouldThrowExceptionWhenUsernameIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new User(userId, null, email, passwordHash)
            );
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new User(userId, username, null, passwordHash)
            );
        }

        @Test
        @DisplayName("Should throw exception when passwordHash is null")
        void shouldThrowExceptionWhenPasswordHashIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new User(userId, username, email, null)
            );
        }
    }

    @Nested
    @DisplayName("User Status Tests")
    class UserStatusTests {

        @Test
        @DisplayName("Should update status to ONLINE and set lastSeenAt")
        void shouldUpdateStatusToOnlineAndSetLastSeenAt() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            Instant beforeUpdate = Instant.now();

            // When
            user.updateStatus(UserStatus.ONLINE);

            // Then
            assertEquals(UserStatus.ONLINE, user.getStatus());
            assertNotNull(user.getLastSeenAt());
            assertTrue(user.getLastSeenAt().isAfter(beforeUpdate) ||
                      user.getLastSeenAt().equals(beforeUpdate));
        }

        @Test
        @DisplayName("Should update status to AWAY and set lastSeenAt")
        void shouldUpdateStatusToAwayAndSetLastSeenAt() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // When
            user.updateStatus(UserStatus.AWAY);

            // Then
            assertEquals(UserStatus.AWAY, user.getStatus());
            assertNotNull(user.getLastSeenAt());
        }

        @Test
        @DisplayName("Should update status to BUSY and set lastSeenAt")
        void shouldUpdateStatusToBusyAndSetLastSeenAt() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // When
            user.updateStatus(UserStatus.BUSY);

            // Then
            assertEquals(UserStatus.BUSY, user.getStatus());
            assertNotNull(user.getLastSeenAt());
        }

        @Test
        @DisplayName("Should update status to OFFLINE without updating lastSeenAt")
        void shouldUpdateStatusToOfflineWithoutUpdatingLastSeenAt() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            user.updateStatus(UserStatus.ONLINE);
            Instant lastSeenWhenOnline = user.getLastSeenAt();

            // When
            user.updateStatus(UserStatus.OFFLINE);

            // Then
            assertEquals(UserStatus.OFFLINE, user.getStatus());
            assertEquals(lastSeenWhenOnline, user.getLastSeenAt());
        }

        @Test
        @DisplayName("Should throw exception when status is null")
        void shouldThrowExceptionWhenStatusIsNull() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // When & Then
            assertThrows(NullPointerException.class, () ->
                user.updateStatus(null)
            );
        }
    }

    @Nested
    @DisplayName("User Profile Tests")
    class UserProfileTests {

        @Test
        @DisplayName("Should update profile with valid data")
        void shouldUpdateProfileWithValidData() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            String displayName = "Test User";
            String avatarUrl = "https://example.com/avatar.jpg";
            String bio = "This is my bio";

            // When
            user.updateProfile(displayName, avatarUrl, bio);

            // Then
            assertEquals(displayName, user.getDisplayName());
            assertEquals(avatarUrl, user.getAvatarUrl());
            assertEquals(bio, user.getBio());
        }

        @Test
        @DisplayName("Should update profile with null values")
        void shouldUpdateProfileWithNullValues() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // When
            user.updateProfile(null, null, null);

            // Then
            assertNull(user.getDisplayName());
            assertNull(user.getAvatarUrl());
            assertNull(user.getBio());
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {

        @Test
        @DisplayName("Should change password with valid hash")
        void shouldChangePasswordWithValidHash() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            String newPasswordHash = "$2a$10$newhashedpassword";

            // When
            user.changePassword(newPasswordHash);

            // Then
            assertEquals(newPasswordHash, user.getPasswordHash());
        }

        @Test
        @DisplayName("Should throw exception when new password hash is null")
        void shouldThrowExceptionWhenNewPasswordHashIsNull() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // When & Then
            assertThrows(NullPointerException.class, () ->
                user.changePassword(null)
            );
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email")
        void shouldVerifyEmail() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            assertFalse(user.isEmailVerified());

            // When
            user.verifyEmail();

            // Then
            assertTrue(user.isEmailVerified());
        }

        @Test
        @DisplayName("Should be active after email verification and not blocked")
        void shouldBeActiveAfterEmailVerificationAndNotBlocked() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // When
            user.verifyEmail();

            // Then
            assertTrue(user.isActive());
        }
    }

    @Nested
    @DisplayName("Block/Unblock Tests")
    class BlockUnblockTests {

        @Test
        @DisplayName("Should block user and set status to OFFLINE")
        void shouldBlockUserAndSetStatusToOffline() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            user.updateStatus(UserStatus.ONLINE);

            // When
            user.block();

            // Then
            assertTrue(user.isBlocked());
            assertEquals(UserStatus.OFFLINE, user.getStatus());
            assertFalse(user.isActive());
        }

        @Test
        @DisplayName("Should unblock user")
        void shouldUnblockUser() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            user.block();

            // When
            user.unblock();

            // Then
            assertFalse(user.isBlocked());
        }

        @Test
        @DisplayName("Should not be active when blocked even if email verified")
        void shouldNotBeActiveWhenBlockedEvenIfEmailVerified() {
            // Given
            User user = new User(userId, username, email, passwordHash);
            user.verifyEmail();

            // When
            user.block();

            // Then
            assertFalse(user.isActive());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same userId")
        void shouldBeEqualWhenSameUserId() {
            // Given
            User user1 = new User(userId, username, email, passwordHash);
            User user2 = new User(userId, new Username("different"),
                                 new Email("different@example.com"), "differenthash");

            // Then
            assertEquals(user1, user2);
            assertEquals(user1.hashCode(), user2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different userId")
        void shouldNotBeEqualWhenDifferentUserId() {
            // Given
            User user1 = new User(userId, username, email, passwordHash);
            User user2 = new User(new UserId(UUID.randomUUID()), username, email, passwordHash);

            // Then
            assertNotEquals(user1, user2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // Then
            assertEquals(user, user);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // Then
            assertNotEquals(null, user);
        }
    }

    @Nested
    @DisplayName("Full Constructor Tests")
    class FullConstructorTests {

        @Test
        @DisplayName("Should reconstitute user from persistence with full constructor")
        void shouldReconstituteUserFromPersistence() {
            // Given
            String displayName = "Test User";
            String avatarUrl = "https://example.com/avatar.jpg";
            String bio = "My bio";
            UserStatus status = UserStatus.ONLINE;
            boolean blocked = false;
            boolean emailVerified = true;
            Instant createdAt = Instant.now().minusSeconds(3600);
            Instant lastSeenAt = Instant.now();

            // When
            User user = new User(userId, username, email, passwordHash,
                displayName, avatarUrl, bio, status, blocked, emailVerified,
                createdAt, lastSeenAt);

            // Then
            assertEquals(userId, user.getId());
            assertEquals(username, user.getUsername());
            assertEquals(email, user.getEmail());
            assertEquals(passwordHash, user.getPasswordHash());
            assertEquals(displayName, user.getDisplayName());
            assertEquals(avatarUrl, user.getAvatarUrl());
            assertEquals(bio, user.getBio());
            assertEquals(status, user.getStatus());
            assertEquals(blocked, user.isBlocked());
            assertEquals(emailVerified, user.isEmailVerified());
            assertEquals(createdAt, user.getCreatedAt());
            assertEquals(lastSeenAt, user.getLastSeenAt());
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain key user information in toString")
        void shouldContainKeyUserInformationInToString() {
            // Given
            User user = new User(userId, username, email, passwordHash);

            // When
            String toString = user.toString();

            // Then
            assertTrue(toString.contains("User{"));
            assertTrue(toString.contains("id="));
            assertTrue(toString.contains("username="));
            assertTrue(toString.contains("email="));
            assertTrue(toString.contains("status="));
            assertTrue(toString.contains("blocked="));
        }
    }
}

