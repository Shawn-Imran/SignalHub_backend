package com.realtime.communication.auth.application.usecase;

import com.realtime.communication.auth.application.port.TokenRepository;
import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.User;
import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.auth.domain.model.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for user logout
 */
@Service
public class LogoutUseCase {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    public LogoutUseCase(UserRepository userRepository, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public void execute(UUID userId) {
        // Update user status to offline
        UserId id = new UserId(userId);
        userRepository.findById(id).ifPresent(user -> {
            user.updateStatus(UserStatus.OFFLINE);
            userRepository.save(user);
        });

        // Revoke all user sessions
        tokenRepository.revokeByUserId(userId);
    }
}
package com.realtime.communication.auth.application.port;

import com.realtime.communication.auth.domain.model.*;

import java.util.Optional;

/**
 * Port interface for User repository
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId userId);
    Optional<User> findByUsername(Username username);
    Optional<User> findByEmail(Email email);
    void delete(UserId userId);
}

