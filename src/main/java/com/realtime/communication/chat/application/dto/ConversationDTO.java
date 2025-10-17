package com.realtime.communication.chat.application.dto;

import com.realtime.communication.chat.domain.model.ConversationType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for Conversation
 */
public record ConversationDTO(
    UUID id,
    ConversationType type,
    Set<UUID> participantIds,
    Instant createdAt,
    Instant lastMessageAt
) {}

