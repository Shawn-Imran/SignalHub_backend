package com.realtime.communication.chat.adapter.out.persistence;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.port.ConversationRepository;
import com.realtime.communication.chat.domain.model.Conversation;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of ConversationRepository
 */
@Repository
public interface JpaConversationRepositoryInterface extends JpaRepository<ConversationJpaEntity, UUID> {
    @Query("SELECT c FROM ConversationJpaEntity c JOIN c.participantIds p WHERE p = :userId")
    List<ConversationJpaEntity> findByParticipantId(UUID userId);
}

@Repository
class JpaConversationRepositoryImpl implements ConversationRepository {

    private final JpaConversationRepositoryInterface jpaRepository;

    public JpaConversationRepositoryImpl(JpaConversationRepositoryInterface jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity entity = toEntity(conversation);
        jpaRepository.save(entity);
        return conversation;
    }

    @Override
    public Optional<Conversation> findById(ConversationId conversationId) {
        return jpaRepository.findById(conversationId.value()).map(this::toDomain);
    }

    @Override
    public List<Conversation> findByParticipant(UserId userId) {
        return jpaRepository.findByParticipantId(userId.value()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(ConversationId conversationId) {
        jpaRepository.deleteById(conversationId.value());
    }

    private ConversationJpaEntity toEntity(Conversation conversation) {
        ConversationJpaEntity entity = new ConversationJpaEntity();
        entity.setId(conversation.getId().value());
        entity.setType(conversation.getType().name());
        entity.setParticipantIds(
            conversation.getParticipants().stream()
                .map(UserId::value)
                .collect(Collectors.toSet())
        );
        entity.setCreatedAt(conversation.getCreatedAt());
        entity.setLastMessageAt(conversation.getLastMessageAt());
        return entity;
    }

    private Conversation toDomain(ConversationJpaEntity entity) {
        Set<UserId> participants = entity.getParticipantIds().stream()
            .map(UserId::new)
            .collect(Collectors.toSet());

        return new Conversation(
            new ConversationId(entity.getId()),
            ConversationType.valueOf(entity.getType()),
            participants
        );
    }
}

