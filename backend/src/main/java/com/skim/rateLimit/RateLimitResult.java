package com.skim.rateLimit;

/**
 * Outcome of a per-IP rate limit check.
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds, String message) {

    public static RateLimitResult success() {
        return new RateLimitResult(true, 0, null);
    }

    public static RateLimitResult rejected(long retryAfterSeconds, String message) {
        return new RateLimitResult(false, retryAfterSeconds, message);
    }
}
