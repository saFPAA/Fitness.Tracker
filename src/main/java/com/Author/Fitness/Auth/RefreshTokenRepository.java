package com.Author.Fitness.Auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    @Query("select t from RefreshTokenEntity t where t.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revoked = true where t.user.id = :userId and t.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
