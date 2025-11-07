package com.realtime.communication.chat.domain.model;

import com.realtime.communication.auth.domain.model.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Conversation aggregate root
 */
@Getter
public class Conversation {
    private final ConversationId id;
    private final ConversationType type;
    private final Set<UserId> participants;
    private final Instant createdAt;
    private Instant lastMessageAt;

    // Constructor for creating a new conversation
    public Conversation(ConversationId id, ConversationType type, Set<UserId> participants) {
        this.id = Objects.requireNonNull(id, "Conversation ID cannot be null");
        this.type = Objects.requireNonNull(type, "Conversation type cannot be null");
        this.participants = new HashSet<>(Objects.requireNonNull(participants, "Participants cannot be null"));
        this.createdAt = Instant.now();

        validateParticipants();
    }

    // Full constructor for reconstitution from persistence
    public Conversation(ConversationId id, ConversationType type, Set<UserId> participants,
                       Instant createdAt, Instant lastMessageAt) {
        this.id = id;
        this.type = type;
        this.participants = new HashSet<>(participants);
        this.createdAt = createdAt;
        this.lastMessageAt = lastMessageAt;
    }

    private void validateParticipants() {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("Conversation must have at least one participant");
        }
        if (type == ConversationType.ONE_TO_ONE && participants.size() != 2) {
            throw new IllegalArgumentException("One-to-one conversation must have exactly 2 participants");
        }
    }

    public void updateLastMessageTime() {
        this.lastMessageAt = Instant.now();
    }

    public void addParticipant(UserId userId) {
        if (type == ConversationType.ONE_TO_ONE) {
            throw new IllegalStateException("Cannot add participants to one-to-one conversation");
        }
        this.participants.add(Objects.requireNonNull(userId, "User ID cannot be null"));
    }

    public void removeParticipant(UserId userId) {
        if (type == ConversationType.ONE_TO_ONE) {
            throw new IllegalStateException("Cannot remove participants from one-to-one conversation");
        }
        this.participants.remove(userId);
    }

    public boolean hasParticipant(UserId userId) {
        return participants.contains(userId);
    }

    public Set<UserId> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", type=" + type +
                ", participantCount=" + participants.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}

