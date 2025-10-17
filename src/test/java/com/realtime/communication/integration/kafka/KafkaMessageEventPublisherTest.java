package com.realtime.communication.integration.kafka;

import com.realtime.communication.chat.adapter.out.messaging.KafkaMessageEventPublisher;
import com.realtime.communication.chat.domain.event.MessageSentEvent;
import com.realtime.communication.chat.domain.model.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for KafkaMessageEventPublisher
 * Tests Kafka event publishing with embedded Kafka
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"message-events"})
@DirtiesContext
@DisplayName("KafkaMessageEventPublisher Integration Tests")
class KafkaMessageEventPublisherTest {

    @Autowired
    private KafkaMessageEventPublisher eventPublisher;

    @Test
    @DisplayName("Should publish MessageSentEvent to Kafka")
    void shouldPublishMessageSentEvent() {
        // Given
        MessageSentEvent event = new MessageSentEvent(
            new MessageId(UUID.randomUUID()),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test message"
        );

        // When/Then
        assertDoesNotThrow(() -> eventPublisher.publish(event));
    }

    @Test
    @DisplayName("Should handle null event gracefully")
    void shouldHandleNullEvent() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> eventPublisher.publish(null));
    }
}
package com.realtime.communication.integration.auth;

import com.realtime.communication.auth.adapter.out.persistence.JpaUserRepository;
import com.realtime.communication.auth.adapter.out.persistence.UserJpaEntity;
import com.realtime.communication.auth.domain.model.Email;
import com.realtime.communication.auth.domain.model.User;
import com.realtime.communication.auth.domain.model.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JpaUserRepository
 * Tests database operations with real PostgreSQL via TestContainers
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaUserRepository Integration Tests")
class JpaUserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    @DisplayName("Should save and find user by username")
    void shouldSaveAndFindUserByUsername() {
        // Given
        UserJpaEntity user = createTestUser("testuser", "test@example.com");

        // When
        UserJpaEntity saved = userRepository.save(user);
        Optional<User> found = userRepository.findByUsername(new Username("testuser"));

        // Then
        assertNotNull(saved);
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername().getValue());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        UserJpaEntity user = createTestUser("emailuser", "email@example.com");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail(new Email("email@example.com"));

        // Then
        assertTrue(found.isPresent());
        assertEquals("email@example.com", found.get().getEmail().getValue());
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void shouldReturnEmptyWhenUserNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername(new Username("nonexistent"));

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should enforce unique username constraint")
    void shouldEnforceUniqueUsernameConstraint() {
        // Given
        UserJpaEntity user1 = createTestUser("uniqueuser", "user1@example.com");
        userRepository.save(user1);

        // When/Then
        UserJpaEntity user2 = createTestUser("uniqueuser", "user2@example.com");
        assertThrows(Exception.class, () -> {
            userRepository.save(user2);
            userRepository.flush();
        });
    }

    private UserJpaEntity createTestUser(String username, String email) {
        UserJpaEntity user = new UserJpaEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed_password");
        user.setDisplayName("Test User");
        user.setStatus("OFFLINE");
        return user;
    }
}

