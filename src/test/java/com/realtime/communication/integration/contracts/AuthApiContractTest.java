package com.realtime.communication.integration.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.RegisterRequest;
import com.realtime.communication.auth.application.port.TokenRepository;
import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for Auth REST API
 * Verifies API contracts match OpenAPI specification
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Auth API Contract Tests")
class AuthApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TokenRepository tokenRepository;

    private User testUser;
    private String plainPassword = "SecurePassword123!";

    @BeforeEach
    void setUp() {
        String passwordHash = HashedPassword.fromPlainText(plainPassword).getHash();
        testUser = new User(
            new UserId(UUID.randomUUID()),
            new Username("testuser"),
            new Email("test@example.com"),
            passwordHash
        );
        testUser.verifyEmail();
    }

    @Test
    @DisplayName("POST /api/auth/register - Should register new user with valid request")
    void shouldRegisterNewUserWithValidRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "newuser@example.com",
            "SecurePass123!",
            "New User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.displayName").value("New User"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when username already exists")
    void shouldReturn400WhenUsernameAlreadyExists() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "testuser",
            "new@example.com",
            "SecurePass123!",
            "Test"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when email already exists")
    void shouldReturn400WhenEmailAlreadyExists() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "test@example.com",
            "SecurePass123!",
            "Test"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 with invalid email format")
    void shouldReturn400WithInvalidEmailFormat() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "invalid-email",
            "SecurePass123!",
            "Test"
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 with missing required fields")
    void shouldReturn400WithMissingRequiredFields() throws Exception {
        // Given - Empty request body
        String emptyRequest = "{}";

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should login with valid credentials")
    void shouldLoginWithValidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 with invalid credentials")
    void shouldReturn401WithInvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("testuser", "WrongPassword");

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 for non-existent user")
    void shouldReturn401ForNonExistentUser() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("nonexistent", "password");

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 when user is blocked")
    void shouldReturn401WhenUserIsBlocked() throws Exception {
        // Given
        testUser.block();
        LoginRequest request = new LoginRequest("testuser", plainPassword);

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User account is blocked"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 400 with missing credentials")
    void shouldReturn400WithMissingCredentials() throws Exception {
        // Given
        String invalidRequest = "{}";

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should accept Content-Type: application/json")
    void shouldAcceptApplicationJsonContentType() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "new@example.com",
            "Pass123!",
            "User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Should return 415 for unsupported media type")
    void shouldReturn415ForUnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Should return appropriate CORS headers")
    void shouldReturnAppropriateCorsHeaders() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "user",
            "user@example.com",
            "Pass123!",
            "User"
        );

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", "http://localhost:3000")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Login response should follow contract structure")
    void loginResponseShouldFollowContractStructure() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("testuser", plainPassword);

        when(userRepository.findByUsername(any(Username.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then - Verify exact response structure
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.length()").value(3)); // Exactly 3 fields
    }
}

