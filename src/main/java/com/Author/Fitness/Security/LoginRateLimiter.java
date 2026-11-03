package com.Author.Fitness.Security;

import java.time.Duration;

public interface LoginRateLimiter {

    Duration blockedFor(String username, String clientIp);

    void recordFailure(String username, String clientIp);

    void recordSuccess(String username, String clientIp);

    Duration registrationBlockedFor(String clientIp);

    void recordRegistrationAttempt(String clientIp);

    Duration passwordChangeBlockedFor(Long userId, String clientIp);

    void recordPasswordChangeFailure(Long userId, String clientIp);

    void recordPasswordChangeSuccess(Long userId, String clientIp);
}
