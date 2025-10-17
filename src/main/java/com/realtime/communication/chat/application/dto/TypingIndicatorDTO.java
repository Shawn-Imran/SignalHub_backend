package com.realtime.communication.chat.application.dto;

import java.util.UUID;

/**
 * DTO for Typing Indicator
 */
public record TypingIndicatorDTO(
    UUID conversationId,
    UUID userId,
    boolean isTyping
) {}

