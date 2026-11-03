package com.Author.Fitness.Auth;

import com.Author.Fitness.User.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record RotationResult(UserEntity user, String refreshToken) {
    }

    private final RefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional
    public String create(UserEntity user) {
        String rawToken = generateRawToken();

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays))
                .revoked(false)
                .build();

        repository.save(entity);
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotationResult rotate(String rawToken) {
        RefreshTokenEntity stored = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (stored.isRevoked()) {
            log.error("Refresh token reuse detected for user id: {}", stored.getUser().getId());
            repository.revokeAllByUserId(stored.getUser().getId());
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        stored.setRevoked(true);

        UserEntity user = stored.getUser();
        String newRawToken = create(user);
        return new RotationResult(user, newRawToken);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllByUserId(userId);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> token.setRevoked(true));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
