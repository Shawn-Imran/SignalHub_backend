package com.realtime.communication.chat.adapter.in.websocket;

import com.realtime.communication.auth.application.dto.LoginRequest;
import com.realtime.communication.auth.application.dto.LoginResponse;
import com.realtime.communication.auth.application.dto.RegisterRequest;
import com.realtime.communication.auth.application.usecase.LoginUseCase;
import com.realtime.communication.auth.application.usecase.LogoutUseCase;
import com.realtime.communication.auth.application.usecase.RefreshTokenUseCase;
import com.realtime.communication.auth.application.usecase.RegisterUserUseCase;
import com.realtime.communication.auth.domain.model.User;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

/**
 * WebSocket event handler for STOMP sessions
 */
@Component
public class ChatWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        logger.info("WebSocket connected: sessionId={}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        logger.info("WebSocket disconnected: sessionId={}", sessionId);
    }
}


