package com.Author.Fitness.Security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;


@Slf4j // НОВОЕ
@Service
@RequiredArgsConstructor
public class RedisLoginRateLimiter implements LoginRateLimiter {

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> ACQUIRE_LOGIN_SCRIPT = new DefaultRedisScript<>("""
            local ttl = tonumber(ARGV[1])

            local function blockedMs(key, limit)
                local v = tonumber(redis.call('GET', key) or '0')
                if v >= limit then
                    local t = redis.call('PTTL', key)
                    if t > 0 then return t end
                end
                return 0
            end

            local function hit(key)
                local c = redis.call('INCR', key)
                if c == 1 or redis.call('PTTL', key) < 0 then
                    redis.call('EXPIRE', key, ttl)
                end
                return c
            end

            local wait = math.max(
                blockedMs(KEYS[1], tonumber(ARGV[2])),
                blockedMs(KEYS[2], tonumber(ARGV[3]))
            )
            if wait > 0 then
                return {wait, 0}
            end

            hit(KEYS[1])
            hit(KEYS[2])
            local loginCount = hit(KEYS[3])

            local suspicious = 0
            if loginCount > tonumber(ARGV[4]) then suspicious = 1 end
            return {0, suspicious}
            """, List.class);

    private static final DefaultRedisScript<Long> RELEASE_LOGIN_SCRIPT = new DefaultRedisScript<>("""
            redis.call('DEL', KEYS[1])
            for i = 2, 3 do
                local v = tonumber(redis.call('GET', KEYS[i]) or '0')
                if v <= 1 then
                    redis.call('DEL', KEYS[i])
                else
                    redis.call('DECR', KEYS[i])
                end
            end
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> ACQUIRE_SINGLE_SCRIPT = new DefaultRedisScript<>("""
            local v = tonumber(redis.call('GET', KEYS[1]) or '0')
            if v >= tonumber(ARGV[2]) then
                local t = redis.call('PTTL', KEYS[1])
                if t > 0 then return t end
            end
            local c = redis.call('INCR', KEYS[1])
            if c == 1 or redis.call('PTTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final LoginRateLimitProperties properties;

    @Override
    public Duration acquireLoginAttempt(String username, String clientIp) {
        long ttlSeconds = Math.max(1, properties.getWindow().toSeconds());

        List<?> result = redis.execute(
                ACQUIRE_LOGIN_SCRIPT,
                List.of(
                        pairKey(username, clientIp),
                        ipKey(clientIp),
                        loginKey(username)
                ),
                Long.toString(ttlSeconds),
                Integer.toString(properties.getMaxAttemptsPerLoginIp()),
                Integer.toString(properties.getMaxAttemptsPerIp()),
                Integer.toString(properties.getMaxAttemptsPerLogin())
        );

        if (result == null || result.size() < 2) {
            return Duration.ZERO;
        }

        long waitMs = ((Number) result.get(0)).longValue();
        if (waitMs > 0) {
            return Duration.ofMillis(waitMs);
        }

        if (((Number) result.get(1)).longValue() == 1L) {
            log.warn("Possible distributed login attack, account hash={}",
                    sha256(normalize(username)).substring(0, 12));
        }

        return Duration.ZERO;
    }


    @Override
    public void recordSuccess(String username, String clientIp) {
        redis.execute(
                RELEASE_LOGIN_SCRIPT,
                List.of(
                        pairKey(username, clientIp),
                        ipKey(clientIp),
                        loginKey(username)
                )
        );
    }

    @Override
    public Duration acquireRegistrationAttempt(String clientIp) {
        return acquireSingle(
                registrationKey(clientIp),
                properties.getRegistrationWindow(),
                properties.getMaxRegistrationAttemptsPerIp()
        );
    }

    @Override
    public Duration acquirePasswordChangeAttempt(Long userId, String clientIp) {
        return acquireSingle(
                passwordChangeKey(userId, clientIp),
                properties.getPasswordChangeWindow(),
                properties.getMaxFailedPasswordChangesPerUserIp()
        );
    }

    // БЕЗ ИЗМЕНЕНИЙ
    @Override
    public void recordPasswordChangeSuccess(Long userId, String clientIp) {
        redis.delete(passwordChangeKey(userId, clientIp));
    }

    private Duration acquireSingle(String key, Duration window, int limit) {
        long ttlSeconds = Math.max(1, window.toSeconds());

        Long waitMs = redis.execute(
                ACQUIRE_SINGLE_SCRIPT,
                List.of(key),
                Long.toString(ttlSeconds),
                Integer.toString(limit)
        );

        return (waitMs == null || waitMs <= 0) ? Duration.ZERO : Duration.ofMillis(waitMs);
    }

    private String pairKey(String username, String clientIp) {
        return "fitness:login:pair:" + sha256(
                normalize(username) + "\u0000" + safeIp(clientIp)
        );
    }

    private String loginKey(String username) {
        return "fitness:login:user:" + sha256(normalize(username));
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

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String safeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }

        String ip = clientIp.trim();

        if (ip.indexOf(':') >= 0) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                if (address instanceof Inet6Address) {
                    byte[] prefix = Arrays.copyOf(address.getAddress(), 8);
                    return HexFormat.of().formatHex(prefix) + "/64";
                }
                return address.getHostAddress();
            } catch (UnknownHostException ignored) {

            }
        }

        return ip;
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