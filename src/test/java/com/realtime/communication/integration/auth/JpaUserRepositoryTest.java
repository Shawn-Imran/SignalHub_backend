package com.realtime.communication.integration.auth;

import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JpaUserRepository
 * Tests actual database operations using Testcontainers
 */
@SpringBootTest
@Testcontainers
@DisplayName("JpaUserRepository Integration Tests")
class JpaUserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {

        UserId userId = new UserId(UUID.randomUUID());
        Username username = new Username("testuser");
        Email email = new Email("test@example.com");
        String passwordHash = "$2a$10$hashedpassword";

        testUser = new User(userId, username, email, passwordHash);
    }

    @Test
    @DisplayName("Should save and retrieve user by ID")
    void shouldSaveAndRetrieveUserById() {
        // When
        User savedUser = userRepository.save(testUser);
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());

        // Then
        assertTrue(retrievedUser.isPresent());
        assertEquals(testUser.getId(), retrievedUser.get().getId());
        assertEquals(testUser.getUsername().getValue(), retrievedUser.get().getUsername().getValue());
        assertEquals(testUser.getEmail().getValue(), retrievedUser.get().getEmail().getValue());
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<User> found = userRepository.findByUsername(testUser.getUsername());

        // Then
        assertTrue(found.isPresent());
        assertEquals(testUser.getId(), found.get().getId());
        assertEquals(testUser.getUsername().getValue(), found.get().getUsername().getValue());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<User> found = userRepository.findByEmail(testUser.getEmail());

        // Then
        assertTrue(found.isPresent());
        assertEquals(testUser.getId(), found.get().getId());
        assertEquals(testUser.getEmail().getValue(), found.get().getEmail().getValue());
    }

    @Test
    @DisplayName("Should return empty when username not found")
    void shouldReturnEmptyWhenUsernameNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername(new Username("nonexistent"));

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail(new Email("nonexistent@example.com"));

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should update user status")
    void shouldUpdateUserStatus() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When
        savedUser.updateStatus(UserStatus.ONLINE);
        User updatedUser = userRepository.save(savedUser);
        User retrievedUser = userRepository.findById(updatedUser.getId()).orElseThrow();

        // Then
        assertEquals(UserStatus.ONLINE, retrievedUser.getStatus());
        assertNotNull(retrievedUser.getLastSeenAt());
    }

    @Test
    @DisplayName("Should update user profile")
    void shouldUpdateUserProfile() {
        // Given
        User savedUser = userRepository.save(testUser);
        String displayName = "Test User";
        String avatarUrl = "https://example.com/avatar.jpg";
        String bio = "This is my bio";

        // When
        savedUser.updateProfile(displayName, avatarUrl, bio);
        User updatedUser = userRepository.save(savedUser);
        User retrievedUser = userRepository.findById(updatedUser.getId()).orElseThrow();

        // Then
        assertEquals(displayName, retrievedUser.getDisplayName());
        assertEquals(avatarUrl, retrievedUser.getAvatarUrl());
        assertEquals(bio, retrievedUser.getBio());
    }

    @Test
    @DisplayName("Should verify email")
    void shouldVerifyEmail() {
        // Given
        User savedUser = userRepository.save(testUser);
        assertFalse(savedUser.isEmailVerified());

        // When
        savedUser.verifyEmail();
        User updatedUser = userRepository.save(savedUser);
        User retrievedUser = userRepository.findById(updatedUser.getId()).orElseThrow();

        // Then
        assertTrue(retrievedUser.isEmailVerified());
    }

    @Test
    @DisplayName("Should block and unblock user")
    void shouldBlockAndUnblockUser() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When - Block user
        savedUser.block();
        User blockedUser = userRepository.save(savedUser);
        User retrievedBlocked = userRepository.findById(blockedUser.getId()).orElseThrow();

        // Then
        assertTrue(retrievedBlocked.isBlocked());
        assertEquals(UserStatus.OFFLINE, retrievedBlocked.getStatus());

        // When - Unblock user
        retrievedBlocked.unblock();
        User unblockedUser = userRepository.save(retrievedBlocked);
        User retrievedUnblocked = userRepository.findById(unblockedUser.getId()).orElseThrow();

        // Then
        assertFalse(retrievedUnblocked.isBlocked());
    }

    @Test
    @DisplayName("Should change password")
    void shouldChangePassword() {
        // Given
        User savedUser = userRepository.save(testUser);
        String oldPasswordHash = savedUser.getPasswordHash();
        String newPasswordHash = "$2a$10$newhashedpassword";

        // When
        savedUser.changePassword(newPasswordHash);
        User updatedUser = userRepository.save(savedUser);
        User retrievedUser = userRepository.findById(updatedUser.getId()).orElseThrow();

        // Then
        assertNotEquals(oldPasswordHash, retrievedUser.getPasswordHash());
        assertEquals(newPasswordHash, retrievedUser.getPasswordHash());
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Given
        User savedUser = userRepository.save(testUser);
        assertTrue(userRepository.findById(savedUser.getId()).isPresent());

        // When
        userRepository.delete(savedUser.getId());

        // Then
        assertFalse(userRepository.findById(savedUser.getId()).isPresent());
    }

    @Test
    @DisplayName("Should persist user with all fields")
    void shouldPersistUserWithAllFields() {
        // Given
        testUser.updateProfile("Display Name", "https://avatar.url", "Bio text");
        testUser.updateStatus(UserStatus.ONLINE);
        testUser.verifyEmail();

        // When
        User savedUser = userRepository.save(testUser);
        User retrievedUser = userRepository.findById(savedUser.getId()).orElseThrow();

        // Then
        assertEquals(testUser.getId(), retrievedUser.getId());
        assertEquals(testUser.getUsername().getValue(), retrievedUser.getUsername().getValue());
        assertEquals(testUser.getEmail().getValue(), retrievedUser.getEmail().getValue());
        assertEquals(testUser.getPasswordHash(), retrievedUser.getPasswordHash());
        assertEquals(testUser.getDisplayName(), retrievedUser.getDisplayName());
        assertEquals(testUser.getAvatarUrl(), retrievedUser.getAvatarUrl());
        assertEquals(testUser.getBio(), retrievedUser.getBio());
        assertEquals(testUser.getStatus(), retrievedUser.getStatus());
        assertEquals(testUser.isBlocked(), retrievedUser.isBlocked());
        assertEquals(testUser.isEmailVerified(), retrievedUser.isEmailVerified());
        assertNotNull(retrievedUser.getCreatedAt());
        assertNotNull(retrievedUser.getLastSeenAt());
    }

    @Test
    @DisplayName("Should enforce unique username constraint")
    void shouldEnforceUniqueUsernameConstraint() {
        // Given
        userRepository.save(testUser);

        User duplicateUser = new User(
            new UserId(UUID.randomUUID()),
            new Username("testuser"), // Same username
            new Email("different@example.com"),
            "$2a$10$hashedpassword"
        );

        // When & Then
        assertThrows(Exception.class, () ->
            userRepository.save(duplicateUser)
        );
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void shouldEnforceUniqueEmailConstraint() {
        // Given
        userRepository.save(testUser);

        User duplicateUser = new User(
            new UserId(UUID.randomUUID()),
            new Username("differentuser"),
            new Email("test@example.com"), // Same email
            "$2a$10$hashedpassword"
        );

        // When & Then
        assertThrows(Exception.class, () ->
            userRepository.save(duplicateUser)
        );
    }

    @Test
    @DisplayName("Should handle multiple users")
    void shouldHandleMultipleUsers() {
        // Given
        User user1 = new User(
            new UserId(UUID.randomUUID()),
            new Username("user1"),
            new Email("user1@example.com"),
            "$2a$10$hashedpassword1"
        );
        User user2 = new User(
            new UserId(UUID.randomUUID()),
            new Username("user2"),
            new Email("user2@example.com"),
            "$2a$10$hashedpassword2"
        );

        // When
        userRepository.save(user1);
        userRepository.save(user2);

        // Then - Verify both users can be found
        assertTrue(userRepository.findByUsername(new Username("user1")).isPresent());
        assertTrue(userRepository.findByUsername(new Username("user2")).isPresent());
        assertTrue(userRepository.findByEmail(new Email("user1@example.com")).isPresent());
        assertTrue(userRepository.findByEmail(new Email("user2@example.com")).isPresent());
    }
}

