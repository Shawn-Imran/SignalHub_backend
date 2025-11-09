package com.realtime.communication.integration.redis;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.adapter.out.messaging.RedisPresenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RedisPresenceAdapter
 * Tests actual Redis operations using Testcontainers
 */
@SpringBootTest
@Testcontainers
@DisplayName("RedisPresenceAdapter Integration Tests")
class RedisPresenceAdapterTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private RedisPresenceAdapter presenceAdapter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private UserId userId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        userId = new UserId(UUID.randomUUID());
        conversationId = UUID.randomUUID();

        // Clear Redis before each test
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Should mark user as typing")
    void shouldMarkUserAsTyping() {
        // When
        presenceAdapter.setTyping(userId, conversationId);

        // Then
        assertTrue(presenceAdapter.isTyping(userId, conversationId));
    }

    @Test
    @DisplayName("Should stop user typing")
    void shouldStopUserTyping() {
        // Given
        presenceAdapter.setTyping(userId, conversationId);
        assertTrue(presenceAdapter.isTyping(userId, conversationId));

        // When
        presenceAdapter.stopTyping(userId, conversationId);

        // Then
        assertFalse(presenceAdapter.isTyping(userId, conversationId));
    }

    @Test
    @DisplayName("Should return false for user not typing")
    void shouldReturnFalseForUserNotTyping() {
        // When
        boolean isTyping = presenceAdapter.isTyping(userId, conversationId);

        // Then
        assertFalse(isTyping);
    }

    @Test
    @DisplayName("Should track typing for multiple users")
    void shouldTrackTypingForMultipleUsers() {
        // Given
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        UserId user3 = new UserId(UUID.randomUUID());

        // When
        presenceAdapter.setTyping(user1, conversationId);
        presenceAdapter.setTyping(user2, conversationId);
        // user3 doesn't set typing

        // Then
        assertTrue(presenceAdapter.isTyping(user1, conversationId));
        assertTrue(presenceAdapter.isTyping(user2, conversationId));
        assertFalse(presenceAdapter.isTyping(user3, conversationId));
    }

    @Test
    @DisplayName("Should isolate typing by conversation")
    void shouldIsolateTypingByConversation() {
        // Given
        UUID conversation1 = UUID.randomUUID();
        UUID conversation2 = UUID.randomUUID();

        // When
        presenceAdapter.setTyping(userId, conversation1);

        // Then
        assertTrue(presenceAdapter.isTyping(userId, conversation1));
        assertFalse(presenceAdapter.isTyping(userId, conversation2));
    }

    @Test
    @DisplayName("Should allow user to type in multiple conversations")
    void shouldAllowUserToTypeInMultipleConversations() {
        // Given
        UUID conversation1 = UUID.randomUUID();
        UUID conversation2 = UUID.randomUUID();

        // When
        presenceAdapter.setTyping(userId, conversation1);
        presenceAdapter.setTyping(userId, conversation2);

        // Then
        assertTrue(presenceAdapter.isTyping(userId, conversation1));
        assertTrue(presenceAdapter.isTyping(userId, conversation2));
    }

    @Test
    @DisplayName("Should stop typing in specific conversation only")
    void shouldStopTypingInSpecificConversationOnly() {
        // Given
        UUID conversation1 = UUID.randomUUID();
        UUID conversation2 = UUID.randomUUID();

        presenceAdapter.setTyping(userId, conversation1);
        presenceAdapter.setTyping(userId, conversation2);

        // When
        presenceAdapter.stopTyping(userId, conversation1);

        // Then
        assertFalse(presenceAdapter.isTyping(userId, conversation1));
        assertTrue(presenceAdapter.isTyping(userId, conversation2));
    }

    @Test
    @DisplayName("Should handle typing indicator TTL")
    void shouldHandleTypingIndicatorTtl() {
        // Given
        presenceAdapter.setTyping(userId, conversationId);
        assertTrue(presenceAdapter.isTyping(userId, conversationId));

        // When - Check TTL is set
        String typingKey = "presence:typing:" + conversationId + ":" + userId.getValue();
        Long ttl = redisTemplate.getExpire(typingKey);

        // Then
        assertNotNull(ttl);
        assertTrue(ttl > 0, "TTL should be set for typing indicator");
    }

    @Test
    @DisplayName("Should handle concurrent typing updates")
    void shouldHandleConcurrentTypingUpdates() throws InterruptedException {
        // When - Rapid typing sets and stops
        presenceAdapter.setTyping(userId, conversationId);
        presenceAdapter.stopTyping(userId, conversationId);
        presenceAdapter.setTyping(userId, conversationId);

        Thread.sleep(50); // Small delay to ensure updates are processed

        // Then - Last action should be reflected
        assertTrue(presenceAdapter.isTyping(userId, conversationId));
    }

    @Test
    @DisplayName("Should clear typing when stopping")
    void shouldClearTypingWhenStopping() {
        // Given
        presenceAdapter.setTyping(userId, conversationId);

        // When
        presenceAdapter.stopTyping(userId, conversationId);

        // Then
        assertFalse(presenceAdapter.isTyping(userId, conversationId));
    }

    @Test
    @DisplayName("Should handle stop typing when not typing")
    void shouldHandleStopTypingWhenNotTyping() {
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> presenceAdapter.stopTyping(userId, conversationId));
        assertFalse(presenceAdapter.isTyping(userId, conversationId));
    }

    @Test
    @DisplayName("Should track typing for different users in same conversation")
    void shouldTrackTypingForDifferentUsersInSameConversation() {
        // Given
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        UserId user3 = new UserId(UUID.randomUUID());

        // When
        presenceAdapter.setTyping(user1, conversationId);
        presenceAdapter.setTyping(user2, conversationId);

        // Then
        assertTrue(presenceAdapter.isTyping(user1, conversationId));
        assertTrue(presenceAdapter.isTyping(user2, conversationId));
        assertFalse(presenceAdapter.isTyping(user3, conversationId));

        // When - Stop one user
        presenceAdapter.stopTyping(user1, conversationId);

        // Then
        assertFalse(presenceAdapter.isTyping(user1, conversationId));
        assertTrue(presenceAdapter.isTyping(user2, conversationId));
    }
}
