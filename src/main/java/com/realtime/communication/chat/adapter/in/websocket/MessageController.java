package com.realtime.communication.chat.adapter.in.websocket;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.MessageDTO;
import com.realtime.communication.chat.application.usecase.SendMessageUseCase;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.MessageType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket controller for message handling
 */
@Controller
public class MessageController {

    private final SendMessageUseCase sendMessageUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(SendMessageUseCase sendMessageUseCase,
                           SimpMessagingTemplate messagingTemplate) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request,
                          @AuthenticationPrincipal String userId) {
        // Send message
        MessageDTO message = sendMessageUseCase.execute(
            new ConversationId(request.conversationId()),
            new UserId(UUID.fromString(userId)),
            request.content(),
            request.type()
        );

        // Broadcast message to conversation participants
        messagingTemplate.convertAndSend(
            "/topic/conversation/" + request.conversationId(),
            message
        );
    }

    private record SendMessageRequest(UUID conversationId, String content, MessageType type) {}
}

