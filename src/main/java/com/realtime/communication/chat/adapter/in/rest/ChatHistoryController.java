package com.realtime.communication.chat.adapter.in.rest;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.MessageDTO;
import com.realtime.communication.chat.application.usecase.LoadConversationHistoryUseCase;
import com.realtime.communication.chat.domain.model.ConversationId;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for chat history endpoints
 */
@RestController
@RequestMapping("/api/v1/conversations")
public class ChatHistoryController {

    private final LoadConversationHistoryUseCase loadConversationHistoryUseCase;

    public ChatHistoryController(LoadConversationHistoryUseCase loadConversationHistoryUseCase) {
        this.loadConversationHistoryUseCase = loadConversationHistoryUseCase;
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Page<MessageDTO>> getConversationHistory(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String userId) {

        Page<MessageDTO> messages = loadConversationHistoryUseCase.execute(
            new ConversationId(conversationId),
            new UserId(UUID.fromString(userId)),
            page,
            size
        );

        return ResponseEntity.ok(messages);
    }
}

