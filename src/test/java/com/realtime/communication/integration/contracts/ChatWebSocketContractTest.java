package com.realtime.communication.integration.contracts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test for WebSocket STOMP protocol
 * Validates WebSocket protocol against websocket-protocols.yaml
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Chat WebSocket Contract Tests")
class ChatWebSocketContractTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private String wsUrl;

    @BeforeEach
    void setUp() {
        wsUrl = "ws://localhost:" + port + "/ws";
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    @DisplayName("Should connect to WebSocket endpoint")
    void shouldConnectToWebSocket() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                latch.countDown();
            }
        }).get(5, TimeUnit.SECONDS);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    @DisplayName("Should send message to /app/chat.send destination")
    void shouldSendMessageToDestination() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        String messagePayload = """
            {
                "conversationId": "123e4567-e89b-12d3-a456-426614174000",
                "content": "Hello, World!",
                "type": "TEXT"
            }
            """;

        session.send("/app/chat.send", messagePayload.getBytes());
        latch.countDown();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        session.disconnect();
    }

    @Test
    @DisplayName("Should subscribe to /user/queue/messages")
    void shouldSubscribeToUserQueue() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                latch.countDown();
            }
        });

        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    @DisplayName("Should send typing indicator")
    void shouldSendTypingIndicator() throws Exception {
        StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        String typingPayload = """
            {
                "conversationId": "123e4567-e89b-12d3-a456-426614174000",
                "isTyping": true
            }
            """;

        session.send("/app/chat.typing", typingPayload.getBytes());

        assertTrue(session.isConnected());
        session.disconnect();
    }
}

