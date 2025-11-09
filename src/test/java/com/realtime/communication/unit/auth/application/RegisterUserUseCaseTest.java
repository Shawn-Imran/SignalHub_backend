package com.realtime.communication.unit.auth.application;

import com.realtime.communication.auth.application.dto.RegisterRequest;
import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.application.usecase.RegisterUserUseCase;
import com.realtime.communication.auth.domain.model.*;
import com.realtime.communication.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegisterUserUseCase
 * Following TDD: These tests verify user registration logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase Tests")
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        registerUserUseCase = new RegisterUserUseCase(userRepository);
    }

    @Test
    @DisplayName("Should successfully register new user with valid data")
    void shouldSuccessfullyRegisterNewUserWithValidData() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123!",
            "Test User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = registerUserUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername().getValue());
        assertEquals("test@example.com", result.getEmail().getValue());
        assertEquals("Test User", result.getDisplayName());
        assertFalse(result.isBlocked());
        assertFalse(result.isEmailVerified());
        assertEquals(UserStatus.OFFLINE, result.getStatus());

        // Verify interactions
        verify(userRepository).findByUsername(any(Username.class));
        verify(userRepository).findByEmail(any(Email.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should save user with hashed password")
    void shouldSaveUserWithHashedPassword() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123!",
            "Test User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registerUserUseCase.execute(request);

        // Then
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser.getPasswordHash());
        assertNotEquals("SecurePassword123!", capturedUser.getPasswordHash());
        assertTrue(capturedUser.getPasswordHash().startsWith("$2a$")); // BCrypt hash
    }

    @Test
    @DisplayName("Should throw ValidationException when username already exists")
    void shouldThrowValidationExceptionWhenUsernameAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "existinguser",
            "test@example.com",
            "SecurePassword123!",
            "Test User"
        );

        User existingUser = new User(
            new UserId(UUID.randomUUID()),
            new Username("existinguser"),
            new Email("other@example.com"),
            "hashedpassword"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(existingUser));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () ->
            registerUserUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("Username already exists"));
        verify(userRepository).findByUsername(any(Username.class));
        verify(userRepository, never()).findByEmail(any(Email.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when email already exists")
    void shouldThrowValidationExceptionWhenEmailAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "existing@example.com",
            "SecurePassword123!",
            "Test User"
        );

        User existingUser = new User(
            new UserId(UUID.randomUUID()),
            new Username("otheruser"),
            new Email("existing@example.com"),
            "hashedpassword"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existingUser));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () ->
            registerUserUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("Email already exists"));
        verify(userRepository).findByUsername(any(Username.class));
        verify(userRepository).findByEmail(any(Email.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should register user with null display name")
    void shouldRegisterUserWithNullDisplayName() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123!",
            null
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = registerUserUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertNull(result.getDisplayName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should check username availability before email")
    void shouldCheckUsernameAvailabilityBeforeEmail() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "existinguser",
            "existing@example.com",
            "SecurePassword123!",
            "Test User"
        );

        User existingUser = new User(
            new UserId(UUID.randomUUID()),
            new Username("existinguser"),
            new Email("other@example.com"),
            "hashedpassword"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThrows(ValidationException.class, () ->
            registerUserUseCase.execute(request)
        );

        // Verify username check happened
        verify(userRepository).findByUsername(any(Username.class));
        // Verify email check never happened (short-circuit)
        verify(userRepository, never()).findByEmail(any(Email.class));
    }

    @Test
    @DisplayName("Should create user with generated UserId")
    void shouldCreateUserWithGeneratedUserId() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123!",
            "Test User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registerUserUseCase.execute(request);

        // Then
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser.getId());
        assertNotNull(capturedUser.getId().getValue());
    }

    @Test
    @DisplayName("Should set user status to OFFLINE on registration")
    void shouldSetUserStatusToOfflineOnRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123!",
            "Test User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = registerUserUseCase.execute(request);

        // Then
        assertEquals(UserStatus.OFFLINE, result.getStatus());
    }

    @Test
    @DisplayName("Should set user as not blocked on registration")
    void shouldSetUserAsNotBlockedOnRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123!",
            "Test User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = registerUserUseCase.execute(request);

        // Then
        assertFalse(result.isBlocked());
    }

    @Test
    @DisplayName("Should set email as not verified on registration")
    void shouldSetEmailAsNotVerifiedOnRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123!",
            "Test User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = registerUserUseCase.execute(request);

        // Then
        assertFalse(result.isEmailVerified());
    }
}

