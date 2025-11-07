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

