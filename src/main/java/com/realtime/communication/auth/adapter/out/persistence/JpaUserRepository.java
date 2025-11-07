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
interface JpaUserRepositoryInterface extends JpaRepository<UserJpaEntity, UUID> {
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
        return jpaRepository.findById(userId.getValue()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.getValue()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue()).map(this::toDomain);
    }

    @Override
    public void delete(UserId userId) {
        jpaRepository.deleteById(userId.getValue());
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().getValue());
        entity.setUsername(user.getUsername().getValue());
        entity.setEmail(user.getEmail().getValue());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setDisplayName(user.getDisplayName());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setBio(user.getBio());
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

        return new User(
            id,
            username,
            email,
            entity.getPasswordHash(),
            entity.getDisplayName(),
            entity.getAvatarUrl(),
            entity.getBio(),
            UserStatus.valueOf(entity.getStatus()),
            entity.isBlocked(),
            entity.isEmailVerified(),
            entity.getCreatedAt(),
            entity.getLastSeenAt()
        );
    }
}
