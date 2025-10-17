package com.realtime.communication.integration.redis;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.adapter.out.messaging.RedisPresenceAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RedisPresenceAdapter
 * Tests Redis operations with real Redis via TestContainers
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
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisPresenceAdapter presenceAdapter;

    @Test
    @DisplayName("Should set user online status")
    void shouldSetUserOnline() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());

        // When
        presenceAdapter.setOnline(userId);
        boolean isOnline = presenceAdapter.isOnline(userId);

        // Then
        assertTrue(isOnline);
    }

    @Test
    @DisplayName("Should set user offline status")
    void shouldSetUserOffline() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        presenceAdapter.setOnline(userId);

        // When
        presenceAdapter.setOffline(userId);
        boolean isOnline = presenceAdapter.isOnline(userId);

        // Then
        assertFalse(isOnline);
    }

    @Test
    @DisplayName("Should get online users")
    void shouldGetOnlineUsers() {
        // Given
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        presenceAdapter.setOnline(user1);
        presenceAdapter.setOnline(user2);

        // When
        Set<UserId> onlineUsers = presenceAdapter.getOnlineUsers();

        // Then
        assertNotNull(onlineUsers);
        assertTrue(onlineUsers.contains(user1));
        assertTrue(onlineUsers.contains(user2));
    }

    @Test
    @DisplayName("Should track typing indicator")
    void shouldTrackTypingIndicator() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        UUID conversationId = UUID.randomUUID();

        // When
        presenceAdapter.setTyping(userId, conversationId);
        boolean isTyping = presenceAdapter.isTyping(userId, conversationId);

        // Then
        assertTrue(isTyping);
    }
}

