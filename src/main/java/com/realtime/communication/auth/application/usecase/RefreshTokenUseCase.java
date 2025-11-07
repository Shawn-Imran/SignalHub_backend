package com.realtime.communication.auth.application.usecase;

import com.realtime.communication.auth.application.dto.LoginResponse;
import com.realtime.communication.auth.application.port.TokenRepository;
import com.realtime.communication.auth.domain.model.UserSession;
import com.realtime.communication.auth.infrastructure.security.JwtTokenProvider;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case for refreshing access token
 */
@Service
public class RefreshTokenUseCase {
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenUseCase(TokenRepository tokenRepository, JwtTokenProvider jwtTokenProvider) {
        this.tokenRepository = tokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse execute(String refreshToken) {
        // Find session by refresh token
        UserSession session = tokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Validate session
        if (!session.isValid()) {
            throw new UnauthorizedException("Session expired or revoked");
        }

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(session.getUserId().getValue().toString());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        Long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs();

        // Update session
        Instant expiresAt = Instant.now().plusMillis(expiresIn);
        UserSession newSession = new UserSession(
            UUID.randomUUID(),
            session.getUserId(),
            newAccessToken,
            newRefreshToken,
            session.getDeviceInfo(),
            expiresAt
        );

        // Revoke old session
        session.revoke();
        tokenRepository.save(session);
        tokenRepository.save(newSession);

        return new LoginResponse(newAccessToken, newRefreshToken, expiresIn);
    }
}

