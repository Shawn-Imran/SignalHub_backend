package com.realtime.communication.auth.application.dto;

/**
 * DTO for token information
 */
public record TokenDTO(
    String token,
    Long expiresIn
) {}

