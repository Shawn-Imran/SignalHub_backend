package com.realtime.communication.chat.adapter.out.persistence;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.port.MessageRepository;
import com.realtime.communication.chat.domain.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository interface for Message
 */
interface JpaMessageRepositoryInterface extends JpaRepository<MessageJpaEntity, UUID> {
    Page<MessageJpaEntity> findByConversationId(UUID conversationId, Pageable pageable);
}

/**
 * JPA implementation of MessageRepository
 */
@Repository
class JpaMessageRepositoryImpl implements MessageRepository {

    private final JpaMessageRepositoryInterface jpaRepository;

    public JpaMessageRepositoryImpl(JpaMessageRepositoryInterface jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Message save(Message message) {
        MessageJpaEntity entity = toEntity(message);
        jpaRepository.save(entity);
        return message;
    }

    @Override
    public Optional<Message> findById(MessageId messageId) {
        return jpaRepository.findById(messageId.getValue()).map(this::toDomain);
    }

    @Override
    public Page<Message> findByConversationId(ConversationId conversationId, Pageable pageable) {
        return jpaRepository.findByConversationId(conversationId.getValue(), pageable)
            .map(this::toDomain);
    }

    @Override
    public void delete(MessageId messageId) {
        jpaRepository.deleteById(messageId.getValue());
    }

    private MessageJpaEntity toEntity(Message message) {
        MessageJpaEntity entity = new MessageJpaEntity();
        entity.setId(message.getId().getValue());
        entity.setConversationId(message.getConversationId().getValue());
        entity.setSenderId(message.getSenderId().getValue());
        entity.setContent(message.getContent());
        entity.setType(message.getType().name());
        entity.setStatus(message.getStatus().name());
        entity.setSentAt(message.getSentAt());
        entity.setDeliveredAt(message.getDeliveredAt());
        entity.setReadAt(message.getReadAt());
        entity.setEdited(message.isEdited());
        entity.setEditedAt(message.getEditedAt());
        return entity;
    }

    private Message toDomain(MessageJpaEntity entity) {
        return new Message(
            new MessageId(entity.getId()),
            new ConversationId(entity.getConversationId()),
            new UserId(entity.getSenderId()),
            entity.getContent(),
            MessageType.valueOf(entity.getType()),
            MessageStatus.valueOf(entity.getStatus()),
            null, // attachments - to be loaded separately if needed
            entity.getSentAt(),
            entity.getDeliveredAt(),
            entity.getReadAt(),
            entity.isEdited(),
            entity.getEditedAt()
        );
    }
}

