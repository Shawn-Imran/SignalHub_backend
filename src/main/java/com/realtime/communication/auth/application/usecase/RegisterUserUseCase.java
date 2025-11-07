package com.realtime.communication.auth.application.usecase;

import com.realtime.communication.auth.application.dto.RegisterRequest;
import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.*;
import com.realtime.communication.shared.domain.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for user registration
 */
@Service
public class RegisterUserUseCase {
    private final UserRepository userRepository;

    public RegisterUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User execute(RegisterRequest request) {
        // Check if username already exists
        Username username = new Username(request.username());
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ValidationException("Username already exists");
        }

        // Check if email already exists
        Email email = new Email(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("Email already exists");
        }

        // Create new user
        UserId userId = UserId.generate();
        HashedPassword password = HashedPassword.fromPlainText(request.password());

        User user = new User(userId, username, email, password.getHash());
        user.updateProfile(request.displayName(), null, null);

        // Save user
        return userRepository.save(user);
    }
}
