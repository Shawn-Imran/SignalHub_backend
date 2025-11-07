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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID; /**
 * REST controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                         LoginUseCase loginUseCase,
                         LogoutUseCase logoutUseCase,
                         RefreshTokenUseCase refreshTokenUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal String userId) {
        logoutUseCase.execute(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        LoginResponse response = refreshTokenUseCase.execute(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId().getValue(),
            user.getUsername().getValue(),
            user.getEmail().getValue(),
            user.getDisplayName()
        );
    }

    private record RefreshTokenRequest(String refreshToken) {}

    private record UserResponse(UUID id, String username, String email, String displayName) {}
}
