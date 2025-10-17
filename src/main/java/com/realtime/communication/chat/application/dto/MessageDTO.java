package com.realtime.communication.chat.application.dto;

import com.realtime.communication.chat.domain.model.MessageStatus;
import com.realtime.communication.chat.domain.model.MessageType;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Message
 */
public record MessageDTO(
    UUID id,
    UUID conversationId,
    UUID senderId,
    String content,
    MessageType type,
    MessageStatus status,
    Instant sentAt,
    Instant deliveredAt,
    Instant readAt,
    boolean edited,
    Instant editedAt
) {}

