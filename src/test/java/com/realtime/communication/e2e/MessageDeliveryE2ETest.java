package com.realtime.communication.e2e;

import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.LoginResponse;
import com.realtime.communication.auth.application.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End test for complete message delivery flow
 * Tests the complete user journey from registration to message exchange
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Message Delivery E2E Tests")
class MessageDeliveryE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Complete flow: Register -> Login -> Send Message -> Receive Message")
    void completeMessageDeliveryFlow() throws Exception {
        // Step 1: Register two users
        RegisterRequest user1Register = new RegisterRequest(
            "user1", "user1@example.com", "SecurePass123!", "User One"
        );
        RegisterRequest user2Register = new RegisterRequest(
            "user2", "user2@example.com", "SecurePass123!", "User Two"
        );

        ResponseEntity<String> user1Response = restTemplate.postForEntity(
            "/api/v1/auth/register", user1Register, String.class
        );
        ResponseEntity<String> user2Response = restTemplate.postForEntity(
            "/api/v1/auth/register", user2Register, String.class
        );

        assertEquals(HttpStatus.CREATED, user1Response.getStatusCode());
        assertEquals(HttpStatus.CREATED, user2Response.getStatusCode());

        // Step 2: Login both users
        LoginRequest user1Login = new LoginRequest("user1", "SecurePass123!");
        LoginRequest user2Login = new LoginRequest("user2", "SecurePass123!");

        ResponseEntity<LoginResponse> user1LoginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login", user1Login, LoginResponse.class
        );
        ResponseEntity<LoginResponse> user2LoginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login", user2Login, LoginResponse.class
        );

        assertEquals(HttpStatus.OK, user1LoginResponse.getStatusCode());
        assertEquals(HttpStatus.OK, user2LoginResponse.getStatusCode());

        String user1Token = user1LoginResponse.getBody().getAccessToken();
        String user2Token = user2LoginResponse.getBody().getAccessToken();

        // Step 3: Create conversation
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String conversationRequest = """
            {
                "participantIds": ["user2-id"],
                "type": "ONE_TO_ONE"
            }
            """;

        ResponseEntity<String> conversationResponse = restTemplate.exchange(
            "/api/v1/conversations",
            HttpMethod.POST,
            new HttpEntity<>(conversationRequest, headers),
            String.class
        );

        assertEquals(HttpStatus.CREATED, conversationResponse.getStatusCode());

        // Step 4: Connect User 2 to WebSocket and subscribe to messages
        String wsUrl = "ws://localhost:" + port + "/ws";
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        StompSession user2Session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        user2Session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessage.set((String) payload);
                messageLatch.countDown();
            }
        });

        // Step 5: User 1 sends message via WebSocket
        StompSession user1Session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        String messagePayload = """
            {
                "conversationId": "test-conversation-id",
                "content": "Hello from User 1!",
                "type": "TEXT"
            }
            """;

        user1Session.send("/app/chat.send", messagePayload.getBytes());

        // Step 6: Verify User 2 receives the message within 1 second (per requirements)
        assertTrue(messageLatch.await(1, TimeUnit.SECONDS),
            "Message should be delivered in less than 1 second");
        assertNotNull(receivedMessage.get());
        assertTrue(receivedMessage.get().contains("Hello from User 1!"));

        // Cleanup
        user1Session.disconnect();
        user2Session.disconnect();
    }

    @Test
    @DisplayName("Should support typing indicators")
    void shouldSupportTypingIndicators() throws Exception {
        // Setup WebSocket connection
        String wsUrl = "ws://localhost:" + port + "/ws";
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch typingLatch = new CountDownLatch(1);

        StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        // Subscribe to typing indicators
        session.subscribe("/topic/conversation/typing", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                typingLatch.countDown();
            }
        });

        // Send typing indicator
        String typingPayload = """
            {
                "conversationId": "test-conversation-id",
                "isTyping": true
            }
            """;

        session.send("/app/chat.typing", typingPayload.getBytes());

        assertTrue(typingLatch.await(2, TimeUnit.SECONDS));
        session.disconnect();
    }

    @Test
    @DisplayName("Should load conversation history via REST API")
    void shouldLoadConversationHistory() {
        // Register and login
        RegisterRequest registerRequest = new RegisterRequest(
            "historyuser", "history@example.com", "SecurePass123!", "History User"
        );
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

        LoginRequest loginRequest = new LoginRequest("historyuser", "SecurePass123!");
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login", loginRequest, LoginResponse.class
        );

        String token = loginResponse.getBody().getAccessToken();

        // Load conversation history
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> historyResponse = restTemplate.exchange(
            "/api/v1/conversations/test-conversation-id/messages?page=0&size=20",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertEquals(HttpStatus.OK, historyResponse.getStatusCode());
    }
}
package com.realtime.communication.integration.contracts;

import com.realtime.communication.auth.adapter.in.rest.AuthController;
import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test for Auth REST API
 * Validates API contract against auth-api.yaml specification
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Auth API Contract Tests")
class AuthApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/auth/register - should match contract")
    void registerEndpointShouldMatchContract() throws Exception {
        String requestBody = """
            {
                "username": "newuser",
                "email": "newuser@example.com",
                "password": "SecurePass123!",
                "displayName": "New User"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - should match contract")
    void loginEndpointShouldMatchContract() throws Exception {
        String requestBody = """
            {
                "username": "testuser",
                "password": "SecurePass123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - should validate request body")
    void registerShouldValidateRequestBody() throws Exception {
        String invalidRequest = """
            {
                "username": "ab",
                "email": "invalid-email",
                "password": "weak"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - should return 401 for invalid credentials")
    void loginShouldReturn401ForInvalidCredentials() throws Exception {
        String requestBody = """
            {
                "username": "testuser",
                "password": "WrongPassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}

