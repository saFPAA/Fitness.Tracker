package com.Author.Fitness.Exception;

import java.time.Duration;

public class TooManyAttemptsException extends RuntimeException {
    private final long retryAfterSeconds;

    public TooManyAttemptsException(Duration retryAfter) {
        super("Too many attempts. Try again later.");
        this.retryAfterSeconds = Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
