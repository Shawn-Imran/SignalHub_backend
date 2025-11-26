package com.realtime.communication.auth.adapter.in.rest;

import com.realtime.communication.auth.adapter.out.messaging.KafkaAuthEventPublisher;
import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.LoginResponse;
import com.realtime.communication.auth.application.dto.RegisterRequest;
import com.realtime.communication.auth.application.usecase.LoginUseCase;
import com.realtime.communication.auth.application.usecase.LogoutUseCase;
import com.realtime.communication.auth.application.usecase.RefreshTokenUseCase;
import com.realtime.communication.auth.application.usecase.RegisterUserUseCase;
import com.realtime.communication.auth.domain.event.UserRegisteredEvent;
import com.realtime.communication.auth.domain.model.User;
import com.realtime.communication.shared.domain.exception.ValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for authentication endpoints.
 * Handles user registration, login, logout, and token refresh.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final KafkaAuthEventPublisher eventPublisher;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            LogoutUseCase logoutUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            KafkaAuthEventPublisher eventPublisher) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account with username, email, and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Too many registration attempts")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received for username: {}", request.username());

        try {
            User user = registerUserUseCase.execute(request);

            // Publish user registered event
            UserRegisteredEvent event = new UserRegisteredEvent(
                    user.getId(),
                    user.getUsername().getValue(),
                    user.getEmail().getValue()
            );
            eventPublisher.publishUserRegistered(event);

            logger.info("User registered successfully: userId={}", user.getId().getValue());

            RegisterResponse response = new RegisterResponse(
                    user.getId().getValue().toString(),
                    user.getUsername().getValue(),
                    user.getEmail().getValue(),
                    user.getCreatedAt()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ValidationException e) {
            logger.warn("Registration validation failed: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Too many login attempts")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for username: {}", request.username());

        LoginResponse response = loginUseCase.execute(request);

        logger.info("User logged in successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate current user session and tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal String userId) {
        logger.info("Logout request received for userId: {}", userId);

        logoutUseCase.execute(UUID.fromString(userId));

        logger.info("User logged out successfully: userId={}", userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        logger.info("Token refresh request received");

        LoginResponse response = refreshTokenUseCase.execute(refreshToken);

        TokenResponse tokenResponse = new TokenResponse(
                response.accessToken(),
                "Bearer",
                response.expiresIn()
        );

        logger.info("Token refreshed successfully");
        return ResponseEntity.ok(tokenResponse);
    }

    // Response DTOs
    public record RegisterResponse(
            String userId,
            String username,
            String email,
            Instant createdAt
    ) {}

    public record TokenResponse(
            String accessToken,
            String tokenType,
            Long expiresIn
    ) {}

    public record ErrorResponse(
            String error,
            String message,
            Instant timestamp,
            String path
    ) {}
}

