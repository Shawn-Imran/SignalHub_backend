package com.realtime.communication.chat.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA Entity for Conversation
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
public class ConversationJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String type;

    @ElementCollection
    @CollectionTable(name = "conversation_participants",
                    joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "user_id")
    private Set<UUID> participantIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
