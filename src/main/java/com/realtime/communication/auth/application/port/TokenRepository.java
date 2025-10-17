package com.realtime.communication.auth.application.port;

import com.realtime.communication.auth.domain.model.UserSession;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for Token repository
 */
public interface TokenRepository {
    UserSession save(UserSession session);
    Optional<UserSession> findByRefreshToken(String refreshToken);
    void revokeByUserId(UUID userId);
}

