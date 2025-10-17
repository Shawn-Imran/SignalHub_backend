package com.realtime.communication.auth.adapter.out.persistence;

import com.realtime.communication.auth.application.port.TokenRepository;
import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.auth.domain.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of TokenRepository
 */
@Repository
public interface JpaTokenRepositoryInterface extends JpaRepository<UserSessionJpaEntity, UUID> {
    Optional<UserSessionJpaEntity> findByRefreshToken(String refreshToken);
    
    @Modifying
    @Query("UPDATE UserSessionJpaEntity s SET s.revoked = true WHERE s.userId = :userId")
    void revokeByUserId(UUID userId);
}

@Repository
class JpaTokenRepositoryImpl implements TokenRepository {

    private final JpaTokenRepositoryInterface jpaRepository;

    public JpaTokenRepositoryImpl(JpaTokenRepositoryInterface jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserSession save(UserSession session) {
        UserSessionJpaEntity entity = toEntity(session);
        jpaRepository.save(entity);
        return session;
    }

    @Override
    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        return jpaRepository.findByRefreshToken(refreshToken).map(this::toDomain);
    }

    @Override
    public void revokeByUserId(UUID userId) {
        jpaRepository.revokeByUserId(userId);
    }

    private UserSessionJpaEntity toEntity(UserSession session) {
        UserSessionJpaEntity entity = new UserSessionJpaEntity();
        entity.setId(session.getId());
        entity.setUserId(session.getUserId().value());
        entity.setAccessToken(session.getAccessToken());
        entity.setRefreshToken(session.getRefreshToken());
        entity.setDeviceInfo(session.getDeviceInfo());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setExpiresAt(session.getExpiresAt());
        entity.setRevoked(session.isRevoked());
        return entity;
    }

    private UserSession toDomain(UserSessionJpaEntity entity) {
        return new UserSession(
            entity.getId(),
            new UserId(entity.getUserId()),
            entity.getAccessToken(),
            entity.getRefreshToken(),
            entity.getDeviceInfo(),
            entity.getExpiresAt()
        );
    }
}

