package com.realtime.communication.unit.auth.application;

import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.LoginResponse;
import com.realtime.communication.auth.application.port.TokenRepository;
import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.application.usecase.LoginUseCase;
import com.realtime.communication.auth.domain.model.*;
import com.realtime.communication.auth.infrastructure.security.JwtTokenProvider;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginUseCase (Application Layer)
 * TDD: These tests are written BEFORE implementation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase Tests")
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(userRepository, tokenRepository, jwtTokenProvider);
    }

    @Test
    @DisplayName("Should login user successfully with valid credentials")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "SecurePass123!");
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(new UserId(UUID.randomUUID()));
        when(user.getUsername()).thenReturn(new Username("testuser"));
        when(user.getPassword()).thenReturn(HashedPassword.fromPlainText("SecurePass123!"));
        when(user.isBlocked()).thenReturn(false);

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LoginResponse response = loginUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getExpiresIn());

        verify(userRepository).findByUsername(any());
        verify(user).updateStatus(UserStatus.ONLINE);
        verify(userRepository).save(user);
        verify(tokenRepository).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent", "password");
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(UnauthorizedException.class, () -> loginUseCase.execute(request));
        verify(userRepository).findByUsername(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void shouldThrowExceptionWhenPasswordIncorrect() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "WrongPassword");
        
        User user = mock(User.class);
        when(user.getPassword()).thenReturn(HashedPassword.fromPlainText("CorrectPassword123!"));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));

        // When/Then
        assertThrows(UnauthorizedException.class, () -> loginUseCase.execute(request));
        verify(userRepository).findByUsername(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user is blocked")
    void shouldThrowExceptionWhenUserBlocked() {
        // Given
        LoginRequest request = new LoginRequest("blockeduser", "SecurePass123!");
        
        User user = mock(User.class);
        when(user.getPassword()).thenReturn(HashedPassword.fromPlainText("SecurePass123!"));
        when(user.isBlocked()).thenReturn(true);
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));

        // When/Then
        assertThrows(UnauthorizedException.class, () -> loginUseCase.execute(request));
        verify(userRepository).findByUsername(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create user session on successful login")
    void shouldCreateUserSession() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "SecurePass123!");
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(new UserId(UUID.randomUUID()));
        when(user.getUsername()).thenReturn(new Username("testuser"));
        when(user.getPassword()).thenReturn(HashedPassword.fromPlainText("SecurePass123!"));
        when(user.isBlocked()).thenReturn(false);

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");

        // When
        LoginResponse response = loginUseCase.execute(request);

        // Then
        assertNotNull(response);
        verify(tokenRepository).save(any());
    }
}
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegisterUserUseCase (Application Layer)
 * TDD: These tests are written BEFORE implementation
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
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "newuser@example.com",
            "SecurePass123!",
            "New User"
        );

        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = registerUserUseCase.execute(request);

        // Then
        assertNotNull(result);
        assertEquals("newuser", result.getUsername().getValue());
        assertEquals("newuser@example.com", result.getEmail().getValue());
        assertFalse(result.isEmailVerified());
        assertEquals(UserStatus.OFFLINE, result.getStatus());

        verify(userRepository).findByUsername(any());
        verify(userRepository).findByEmail(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "existinguser",
            "new@example.com",
            "SecurePass123!",
            "New User"
        );

        User existingUser = mock(User.class);
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(existingUser));

        // When/Then
        assertThrows(ValidationException.class, () -> registerUserUseCase.execute(request));
        verify(userRepository).findByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "existing@example.com",
            "SecurePass123!",
            "New User"
        );

        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        User existingUser = mock(User.class);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(existingUser));

        // When/Then
        assertThrows(ValidationException.class, () -> registerUserUseCase.execute(request));
        verify(userRepository).findByUsername(any());
        verify(userRepository).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid password")
    void shouldThrowExceptionForInvalidPassword() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "newuser@example.com",
            "weak",
            "New User"
        );

        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ValidationException.class, () -> registerUserUseCase.execute(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should publish UserRegisteredEvent after successful registration")
    void shouldPublishUserRegisteredEventAfterRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "newuser@example.com",
            "SecurePass123!",
            "New User"
        );

        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = registerUserUseCase.execute(request);

        // Then
        assertNotNull(result);
        // Event publishing will be verified when EventPublisher is integrated
    }
}

