package com.realtime.communication.chat.adapter.in.websocket;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.TypingIndicatorDTO;
import com.realtime.communication.chat.domain.service.TypingIndicatorService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket controller for typing indicators
 */
@Controller
public class TypingIndicatorController {

    private final TypingIndicatorService typingIndicatorService;
    private final SimpMessagingTemplate messagingTemplate;

    public TypingIndicatorController(TypingIndicatorService typingIndicatorService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.typingIndicatorService = typingIndicatorService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicatorRequest request,
                            @AuthenticationPrincipal String userId) {
        UserId user = new UserId(UUID.fromString(userId));

        if (request.isTyping()) {
            typingIndicatorService.startTyping(user, request.conversationId());
        } else {
            typingIndicatorService.stopTyping(user, request.conversationId());
        }

        // Broadcast typing indicator to conversation
        TypingIndicatorDTO dto = new TypingIndicatorDTO(
            request.conversationId(),
            UUID.fromString(userId),
            request.isTyping()
        );

        messagingTemplate.convertAndSend(
            "/topic/conversation/" + request.conversationId() + "/typing",
            dto
        );
    }

    private record TypingIndicatorRequest(UUID conversationId, boolean isTyping) {}
}

