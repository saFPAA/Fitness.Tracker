package com.Author.Fitness.Security;

import java.time.Duration;

public interface LoginRateLimiter {

    Duration acquireLoginAttempt(String username, String clientIp);

    void recordSuccess(String username, String clientIp);

    Duration acquireRegistrationAttempt(String clientIp);

    Duration acquirePasswordChangeAttempt(Long userId, String clientIp);

    void recordPasswordChangeSuccess(Long userId, String clientIp);
}
