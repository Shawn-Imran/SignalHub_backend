package com.realtime.communication.chat.infrastructure.monitoring;

import com.realtime.communication.chat.adapter.out.persistence.MessageJpaEntity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

/**
 * Metrics collector for message operations
 */
@Component
public class MessageMetricsCollector {

    private final Counter messagesSentCounter;
    private final Counter messagesDeliveredCounter;
    private final Counter messagesReadCounter;
    private final Timer messageDeliveryTimer;

    public MessageMetricsCollector(MeterRegistry meterRegistry) {
        this.messagesSentCounter = Counter.builder("messages.sent")
            .description("Total number of messages sent")
            .register(meterRegistry);

        this.messagesDeliveredCounter = Counter.builder("messages.delivered")
            .description("Total number of messages delivered")
            .register(meterRegistry);

        this.messagesReadCounter = Counter.builder("messages.read")
            .description("Total number of messages read")
            .register(meterRegistry);

        this.messageDeliveryTimer = Timer.builder("messages.delivery.time")
            .description("Time taken for message delivery")
            .register(meterRegistry);
    }

    public void incrementMessagesSent() {
        messagesSentCounter.increment();
    }

    public void incrementMessagesDelivered() {
        messagesDeliveredCounter.increment();
    }

    public void incrementMessagesRead() {
        messagesReadCounter.increment();
    }

    public Timer.Sample startDeliveryTimer() {
        return Timer.start();
    }

    public void recordDeliveryTime(Timer.Sample sample) {
        sample.stop(messageDeliveryTimer);
    }
}


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
 * JPA implementation of MessageRepository
 */
@Repository
public interface JpaMessageRepositoryInterface extends JpaRepository<MessageJpaEntity, UUID> {
    Page<MessageJpaEntity> findByConversationId(UUID conversationId, Pageable pageable);
}

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
        return jpaRepository.findById(messageId.value()).map(this::toDomain);
    }

    @Override
    public Page<Message> findByConversationId(ConversationId conversationId, Pageable pageable) {
        return jpaRepository.findByConversationId(conversationId.value(), pageable)
            .map(this::toDomain);
    }

    @Override
    public void delete(MessageId messageId) {
        jpaRepository.deleteById(messageId.value());
    }

    private MessageJpaEntity toEntity(Message message) {
        MessageJpaEntity entity = new MessageJpaEntity();
        entity.setId(message.getId().value());
        entity.setConversationId(message.getConversationId().value());
        entity.setSenderId(message.getSenderId().value());
        entity.setContent(message.getContent());
        entity.setType(message.getType().name());
        entity.setStatus(message.getStatus().name());
        entity.setSentAt(message.getSentAt());
        entity.setDeliveredAt(message.getDeliveredAt());
        entity.setReadAt(message.getReadAt());
        entity.setEdited(message.isEdited());
        entity.setEditedAt(message.getEditedAt());
        entity.setDeleted(message.isDeleted());
        entity.setDeletedAt(message.getDeletedAt());
        return entity;
    }

    private Message toDomain(MessageJpaEntity entity) {
        Message message = new Message(
            new MessageId(entity.getId()),
            new ConversationId(entity.getConversationId()),
            new UserId(entity.getSenderId()),
            entity.getContent(),
            MessageType.valueOf(entity.getType())
        );

        if (entity.getStatus().equals("DELIVERED")) {
            message.markAsDelivered();
        } else if (entity.getStatus().equals("READ")) {
            message.markAsDelivered();
            message.markAsRead();
        }

        return message;
    }
}

