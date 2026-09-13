package com.skim.rateLimit;

import com.skim.config.RateLimitProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-IP request counts using two rolling windows: a short "burst" window
 * (stops rapid-fire clicking/scripts) and a longer "daily" window (stops one
 * person casually eating the whole quota over a day). Dependency-free, in-memory —
 * fine for a single-instance demo app.
 */
@Service
public class RateLimiterService {

    private final RateLimitProperties properties;
    private final Map<String, ClientUsage> usageByIp = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public RateLimitResult checkAndConsume(String clientIp) {
        ClientUsage usage = usageByIp.computeIfAbsent(clientIp, ip -> new ClientUsage());

        synchronized (usage) {
            Instant now = Instant.now();

            if (now.isAfter(usage.burstWindowResetAt)) {
                usage.burstCount = 0;
                usage.burstWindowResetAt = now.plus(Duration.ofMinutes(1));
            }
            if (now.isAfter(usage.dailyWindowResetAt)) {
                usage.dailyCount = 0;
                usage.dailyWindowResetAt = now.plus(Duration.ofDays(1));
            }

            if (usage.burstCount >= properties.getPerIpBurstLimit()) {
                long retryAfter = Math.max(1, Duration.between(now, usage.burstWindowResetAt).getSeconds());
                return RateLimitResult.rejected(retryAfter,
                        "You're sending requests a bit fast. Please wait a moment and try again.");
            }

            if (usage.dailyCount >= properties.getPerIpDailyLimit()) {
                long retryAfter = Math.max(1, Duration.between(now, usage.dailyWindowResetAt).getSeconds());
                return RateLimitResult.rejected(retryAfter,
                        "You've reached today's usage limit for this demo. Please try again tomorrow.");
            }

            usage.burstCount++;
            usage.dailyCount++;
            return RateLimitResult.success();
        }
    }

    private static class ClientUsage {
        int burstCount = 0;
        int dailyCount = 0;
        Instant burstWindowResetAt = Instant.now().plus(Duration.ofMinutes(1));
        Instant dailyWindowResetAt = Instant.now().plus(Duration.ofDays(1));
    }
}
