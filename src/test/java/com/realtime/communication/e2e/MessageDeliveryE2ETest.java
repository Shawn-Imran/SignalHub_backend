package com.realtime.communication.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.LoginResponse;
import com.realtime.communication.auth.application.dto.RegisterRequest;
import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.ConversationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End test for complete message delivery flow
 * Tests the entire flow from user registration to message delivery
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Message Delivery E2E Tests")
class MessageDeliveryE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withKraft();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private String user2Token;
    private String user1Id;
    private String user2Id;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login two users
        user1Token = registerAndLoginUser("alice", "alice@example.com", "AlicePass123!");
        user2Token = registerAndLoginUser("bob", "bob@example.com", "BobPass123!");
    }

    @Test
    @DisplayName("E2E: Complete message delivery flow from sender to recipient")
    void completeMessageDeliveryFlow() throws Exception {
        // Step 1: Alice creates a conversation with Bob
        Map<String, Object> createConversationRequest = new HashMap<>();
        createConversationRequest.put("type", "ONE_TO_ONE");
        createConversationRequest.put("participantIds", new String[]{user1Id, user2Id});

        MvcResult conversationResult = mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createConversationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("ONE_TO_ONE"))
                .andReturn();

        String responseBody = conversationResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> conversationData = objectMapper.readValue(responseBody, Map.class);
        String conversationId = (String) conversationData.get("id");

        assertNotNull(conversationId);

        // Step 2: Alice sends a message to the conversation
        Map<String, Object> sendMessageRequest = new HashMap<>();
        sendMessageRequest.put("conversationId", conversationId);
        sendMessageRequest.put("content", "Hello Bob! How are you?");
        sendMessageRequest.put("type", "TEXT");

        MvcResult messageResult = mockMvc.perform(post("/api/messages")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendMessageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Hello Bob! How are you?"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andReturn();

        String messageResponseBody = messageResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> messageData = objectMapper.readValue(messageResponseBody, Map.class);
        String messageId = (String) messageData.get("id");

        assertNotNull(messageId);

        // Step 3: Bob retrieves the conversation history and sees Alice's message
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + user2Token)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(messageId))
                .andExpect(jsonPath("$.content[0].content").value("Hello Bob! How are you?"))
                .andExpect(jsonPath("$.content[0].senderId").exists());

        // Step 4: Bob sends a reply
        Map<String, Object> replyMessageRequest = new HashMap<>();
        replyMessageRequest.put("conversationId", conversationId);
        replyMessageRequest.put("content", "Hi Alice! I'm doing great, thanks!");
        replyMessageRequest.put("type", "TEXT");

        mockMvc.perform(post("/api/messages")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(replyMessageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Hi Alice! I'm doing great, thanks!"))
                .andExpect(jsonPath("$.status").value("SENT"));

        // Step 5: Alice retrieves conversation history and sees both messages
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + user1Token)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Step 6: Verify conversation's last message timestamp was updated
        mockMvc.perform(get("/api/conversations/" + conversationId)
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.lastMessageAt").exists());
    }

    @Test
    @DisplayName("E2E: Message delivery with offline user queuing")
    void messageDeliveryWithOfflineUserQueuing() throws Exception {
        // Step 1: Create conversation
        Map<String, Object> createConversationRequest = new HashMap<>();
        createConversationRequest.put("type", "ONE_TO_ONE");
        createConversationRequest.put("participantIds", new String[]{user1Id, user2Id});

        MvcResult conversationResult = mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createConversationRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = conversationResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> conversationData = objectMapper.readValue(responseBody, Map.class);
        String conversationId = (String) conversationData.get("id");

        // Step 2: Alice sends message while Bob is "offline"
        Map<String, Object> sendMessageRequest = new HashMap<>();
        sendMessageRequest.put("conversationId", conversationId);
        sendMessageRequest.put("content", "Message sent while you were offline");
        sendMessageRequest.put("type", "TEXT");

        mockMvc.perform(post("/api/messages")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendMessageRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));

        // Step 3: Bob comes online and retrieves messages
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + user2Token)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Message sent while you were offline"));
    }

    @Test
    @DisplayName("E2E: Unauthorized user cannot access conversation")
    void unauthorizedUserCannotAccessConversation() throws Exception {
        // Step 1: Alice creates a conversation with Bob
        Map<String, Object> createConversationRequest = new HashMap<>();
        createConversationRequest.put("type", "ONE_TO_ONE");
        createConversationRequest.put("participantIds", new String[]{user1Id, user2Id});

        MvcResult conversationResult = mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createConversationRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = conversationResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> conversationData = objectMapper.readValue(responseBody, Map.class);
        String conversationId = (String) conversationData.get("id");

        // Step 2: Register a third user (Charlie)
        String charlieToken = registerAndLoginUser("charlie", "charlie@example.com", "CharliePass123!");

        // Step 3: Charlie tries to access Alice and Bob's conversation
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + charlieToken)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("E2E: Message pagination works correctly")
    void messagePaginationWorksCorrectly() throws Exception {
        // Step 1: Create conversation
        Map<String, Object> createConversationRequest = new HashMap<>();
        createConversationRequest.put("type", "ONE_TO_ONE");
        createConversationRequest.put("participantIds", new String[]{user1Id, user2Id});

        MvcResult conversationResult = mockMvc.perform(post("/api/conversations")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createConversationRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = conversationResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> conversationData = objectMapper.readValue(responseBody, Map.class);
        String conversationId = (String) conversationData.get("id");

        // Step 2: Send 10 messages
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> sendMessageRequest = new HashMap<>();
            sendMessageRequest.put("conversationId", conversationId);
            sendMessageRequest.put("content", "Message " + i);
            sendMessageRequest.put("type", "TEXT");

            mockMvc.perform(post("/api/messages")
                    .header("Authorization", "Bearer " + user1Token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sendMessageRequest)))
                    .andExpect(status().isCreated());
        }

        // Step 3: Retrieve first page (5 messages)
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + user2Token)
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        // Step 4: Retrieve second page
        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + user2Token)
                .param("page", "1")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("E2E: Cannot send message without authentication")
    void cannotSendMessageWithoutAuthentication() throws Exception {
        // Given
        String conversationId = UUID.randomUUID().toString();
        Map<String, Object> sendMessageRequest = new HashMap<>();
        sendMessageRequest.put("conversationId", conversationId);
        sendMessageRequest.put("content", "Unauthorized message");
        sendMessageRequest.put("type", "TEXT");

        // When & Then
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sendMessageRequest)))
                .andExpect(status().isUnauthorized());
    }

    // Helper method to register and login a user
    private String registerAndLoginUser(String username, String email, String password) throws Exception {
        // Register
        RegisterRequest registerRequest = new RegisterRequest(username, email, password, username);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String registerResponseBody = registerResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = objectMapper.readValue(registerResponseBody, Map.class);
        String userId = (String) userData.get("id");

        if (user1Id == null) {
            user1Id = userId;
        } else if (user2Id == null) {
            user2Id = userId;
        }

        // Login
        LoginRequest loginRequest = new LoginRequest(username, password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(loginResponseBody, LoginResponse.class);

        return loginResponse.accessToken();
    }
}

