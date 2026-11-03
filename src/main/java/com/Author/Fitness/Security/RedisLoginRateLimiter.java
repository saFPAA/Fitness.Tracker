package com.Author.Fitness.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisLoginRateLimiter implements LoginRateLimiter {

    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT;
    private static final DefaultRedisScript<Long> RECORD_SINGLE_ATTEMPT_SCRIPT;

    static {
        RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>();
        RECORD_FAILURE_SCRIPT.setScriptText("""
                local ttl = tonumber(ARGV[1])

                local pairCount = redis.call('INCR', KEYS[1])
                if pairCount == 1 then
                    redis.call('EXPIRE', KEYS[1], ttl)
                end

                local ipCount = redis.call('INCR', KEYS[2])
                if ipCount == 1 then
                    redis.call('EXPIRE', KEYS[2], ttl)
                end

                return 1
                """);
        RECORD_FAILURE_SCRIPT.setResultType(Long.class);

        RECORD_SINGLE_ATTEMPT_SCRIPT = new DefaultRedisScript<>();
        RECORD_SINGLE_ATTEMPT_SCRIPT.setScriptText("""
                local count = redis.call('INCR', KEYS[1])
                if count == 1 then
                    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
                end
                return count
                """);
        RECORD_SINGLE_ATTEMPT_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final LoginRateLimitProperties properties;

    @Override
    public Duration blockedFor(String username, String clientIp) {
        Duration pairWait = remainingIfBlocked(
                pairKey(username, clientIp),
                properties.getMaxAttemptsPerLoginIp()
        );

        Duration ipWait = remainingIfBlocked(
                ipKey(clientIp),
                properties.getMaxAttemptsPerIp()
        );

        return pairWait.compareTo(ipWait) >= 0 ? pairWait : ipWait;
    }

    @Override
    public void recordFailure(String username, String clientIp) {
        long ttlSeconds = Math.max(1, properties.getWindow().toSeconds());

        redis.execute(
                RECORD_FAILURE_SCRIPT,
                List.of(pairKey(username, clientIp), ipKey(clientIp)),
                Long.toString(ttlSeconds)
        );
    }

    @Override
    public void recordSuccess(String username, String clientIp) {
        redis.delete(pairKey(username, clientIp));
    }

    @Override
    public Duration registrationBlockedFor(String clientIp) {
        return remainingIfBlocked(
                registrationKey(clientIp),
                properties.getMaxRegistrationAttemptsPerIp()
        );
    }

    @Override
    public void recordRegistrationAttempt(String clientIp) {
        recordSingleAttempt(registrationKey(clientIp), properties.getRegistrationWindow());
    }

    @Override
    public Duration passwordChangeBlockedFor(Long userId, String clientIp) {
        return remainingIfBlocked(
                passwordChangeKey(userId, clientIp),
                properties.getMaxFailedPasswordChangesPerUserIp()
        );
    }

    @Override
    public void recordPasswordChangeFailure(Long userId, String clientIp) {
        recordSingleAttempt(
                passwordChangeKey(userId, clientIp),
                properties.getPasswordChangeWindow()
        );
    }

    @Override
    public void recordPasswordChangeSuccess(Long userId, String clientIp) {
        redis.delete(passwordChangeKey(userId, clientIp));
    }

    private void recordSingleAttempt(String key, Duration window) {
        long ttlSeconds = Math.max(1, window.toSeconds());

        redis.execute(
                RECORD_SINGLE_ATTEMPT_SCRIPT,
                List.of(key),
                Long.toString(ttlSeconds)
        );
    }

    private Duration remainingIfBlocked(String key, int limit) {
        String countValue = redis.opsForValue().get(key);

        if (countValue == null || Long.parseLong(countValue) < limit) {
            return Duration.ZERO;
        }

        Long ttlMillis = redis.getExpire(key, TimeUnit.MILLISECONDS);
        if (ttlMillis == null || ttlMillis <= 0) {
            return Duration.ZERO;
        }

        return Duration.ofMillis(ttlMillis);
    }

    private String pairKey(String username, String clientIp) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        return "fitness:login:pair:" + sha256(
                normalizedUsername + "\u0000" + safeIp(clientIp)
        );
    }

    private String ipKey(String clientIp) {
        return "fitness:login:ip:" + sha256(safeIp(clientIp));
    }

    private String registrationKey(String clientIp) {
        return "fitness:register:ip:" + sha256(safeIp(clientIp));
    }

    private String passwordChangeKey(Long userId, String clientIp) {
        return "fitness:password-change:" + sha256(userId + "\u0000" + safeIp(clientIp));
    }

    private String safeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
