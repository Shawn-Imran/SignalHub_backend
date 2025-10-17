package com.realtime.communication.auth.adapter.out.persistence;

import com.realtime.communication.auth.application.port.UserRepository;
import com.realtime.communication.auth.domain.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of UserRepository
 */
@Repository
public interface JpaUserRepositoryInterface extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByEmail(String email);
}

@Repository
class JpaUserRepositoryImpl implements UserRepository {

    private final JpaUserRepositoryInterface jpaRepository;

    public JpaUserRepositoryImpl(JpaUserRepositoryInterface jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return jpaRepository.findById(userId.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public void delete(UserId userId) {
        jpaRepository.deleteById(userId.value());
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().value());
        entity.setUsername(user.getUsername().value());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPassword().getHash());
        entity.setDisplayName(user.getProfile().displayName());
        entity.setAvatarUrl(user.getProfile().avatarUrl());
        entity.setBio(user.getProfile().bio());
        entity.setStatus(user.getStatus().name());
        entity.setBlocked(user.isBlocked());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setLastSeenAt(user.getLastSeenAt());
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        UserId id = new UserId(entity.getId());
        Username username = new Username(entity.getUsername());
        Email email = new Email(entity.getEmail());
        HashedPassword password = HashedPassword.fromHash(entity.getPasswordHash());
        UserProfile profile = new UserProfile(
            entity.getDisplayName(),
            entity.getAvatarUrl(),
            entity.getBio()
        );

        User user = new User(id, username, email, password, profile);
        user.updateStatus(UserStatus.valueOf(entity.getStatus()));
        
        if (entity.isBlocked()) {
            user.block();
        }
        if (entity.isEmailVerified()) {
            user.verifyEmail();
        }

        return user;
    }
}

