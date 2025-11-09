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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginUseCase
 * Following TDD: These tests verify login logic
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

    private User testUser;
    private String plainPassword = "SecurePassword123!";
    private String passwordHash;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(userRepository, tokenRepository, jwtTokenProvider);

        // Create a test user with hashed password
        passwordHash = HashedPassword.fromPlainText(plainPassword).getHash();
        testUser = new User(
            new UserId(UUID.randomUUID()),
            new Username("testuser"),
            new Email("test@example.com"),
            passwordHash
        );
        testUser.verifyEmail(); // Verify email to make user active
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void shouldSuccessfullyLoginWithValidCredentials() {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);
        String accessToken = "access.token.jwt";
        String refreshToken = "refresh.token.jwt";
        Long expiresIn = 3600000L;

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyString())).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(refreshToken);
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(expiresIn);
        when(tokenRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LoginResponse response = loginUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
        assertEquals(expiresIn, response.expiresIn());

        verify(userRepository).findByUsername(any(Username.class));
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateAccessToken(anyString());
        verify(jwtTokenProvider).generateRefreshToken();
        verify(tokenRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Should update user status to ONLINE on successful login")
    void shouldUpdateUserStatusToOnlineOnSuccessfulLogin() {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(anyString())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(testUser);
        when(tokenRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        loginUseCase.execute(request);

        // Then
        User savedUser = userCaptor.getValue();
        assertEquals(UserStatus.ONLINE, savedUser.getStatus());
        assertNotNull(savedUser.getLastSeenAt());
    }

    @Test
    @DisplayName("Should create and save user session on successful login")
    void shouldCreateAndSaveUserSessionOnSuccessfulLogin() {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);
        String accessToken = "access.token.jwt";
        String refreshToken = "refresh.token.jwt";
        Long expiresIn = 3600000L;

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyString())).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken()).thenReturn(refreshToken);
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(expiresIn);

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        when(tokenRepository.save(sessionCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        loginUseCase.execute(request);

        // Then
        UserSession savedSession = sessionCaptor.getValue();
        assertNotNull(savedSession);
        assertEquals(testUser.getId(), savedSession.getUserId());
        assertEquals(accessToken, savedSession.getAccessToken());
        assertEquals(refreshToken, savedSession.getRefreshToken());
        assertNotNull(savedSession.getExpiresAt());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user not found")
    void shouldThrowUnauthorizedExceptionWhenUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent", plainPassword);

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            loginUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("Invalid credentials"));
        verify(userRepository).findByUsername(any(Username.class));
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(tokenRepository, never()).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when password is incorrect")
    void shouldThrowUnauthorizedExceptionWhenPasswordIsIncorrect() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "WrongPassword123!");

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            loginUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("Invalid credentials"));
        verify(userRepository).findByUsername(any(Username.class));
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(tokenRepository, never()).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is blocked")
    void shouldThrowUnauthorizedExceptionWhenUserIsBlocked() {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);
        testUser.block();

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            loginUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("blocked"));
        verify(userRepository).findByUsername(any(Username.class));
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(tokenRepository, never()).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Should generate JWT tokens with user ID")
    void shouldGenerateJwtTokensWithUserId() {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);
        String userId = testUser.getId().getValue().toString();

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(userId)).thenReturn("access.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh.token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);
        when(tokenRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        loginUseCase.execute(request);

        // Then
        verify(jwtTokenProvider).generateAccessToken(userId);
    }

    @Test
    @DisplayName("Should return token expiration time from JWT provider")
    void shouldReturnTokenExpirationTimeFromJwtProvider() {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);
        Long expectedExpiresIn = 7200000L;

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyString())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(expectedExpiresIn);
        when(tokenRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LoginResponse response = loginUseCase.execute(request);

        // Then
        assertEquals(expectedExpiresIn, response.expiresIn());
    }

    @Test
    @DisplayName("Should handle case-sensitive username lookup")
    void shouldHandleCaseSensitiveUsernameLookup() {
        // Given
        LoginRequest request = new LoginRequest("TESTUSER", plainPassword);

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UnauthorizedException.class, () ->
            loginUseCase.execute(request)
        );

        verify(userRepository).findByUsername(any(Username.class));
    }

    @Test
    @DisplayName("Should allow login for email verified user")
    void shouldAllowLoginForEmailVerifiedUser() {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);
        testUser.verifyEmail();

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyString())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);
        when(tokenRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertDoesNotThrow(() -> loginUseCase.execute(request));
        assertTrue(testUser.isEmailVerified());
    }

    @Test
    @DisplayName("Should check if user is blocked before verifying password")
    void shouldCheckIfUserIsBlockedBeforeVerifyingPassword() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "WrongPassword");
        testUser.block();

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            loginUseCase.execute(request)
        );

        // Should fail due to blocked status, not password
        assertTrue(exception.getMessage().contains("blocked"));
    }
}

