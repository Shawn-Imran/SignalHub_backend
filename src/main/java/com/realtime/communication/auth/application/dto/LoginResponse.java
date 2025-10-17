package com.realtime.communication.auth.application.dto;

/**
 * DTO for login response
 */
public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    String tokenType
) {
    public LoginResponse(String accessToken, String refreshToken, Long expiresIn) {
        this(accessToken, refreshToken, expiresIn, "Bearer");
    }
}

