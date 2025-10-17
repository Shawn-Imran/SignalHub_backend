package com.realtime.communication.auth.application.usecase;

import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.LoginResponse;
import com.realtime.communication.auth.application.port.TokenRepository;
import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.*;
import com.realtime.communication.auth.infrastructure.security.JwtTokenProvider;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for user login
 */
@Service
public class LoginUseCase {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginUseCase(UserRepository userRepository, TokenRepository tokenRepository,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse execute(LoginRequest request) {
        // Find user by username
        Username username = new Username(request.username());
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Check if user is blocked
        if (user.isBlocked()) {
            throw new UnauthorizedException("User account is blocked");
        }

        // Verify password
        if (!user.getPassword().matches(request.password())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Update user status to online
        user.updateStatus(UserStatus.ONLINE);
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId().value().toString());
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        Long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs();

        // Create user session
        Instant expiresAt = Instant.now().plusMillis(expiresIn);
        UserSession session = new UserSession(
            UUID.randomUUID(),
            user.getId(),
            accessToken,
            refreshToken,
            "web", // TODO: Extract from request headers
            expiresAt
        );
        tokenRepository.save(session);

        return new LoginResponse(accessToken, refreshToken, expiresIn);
    }
}

