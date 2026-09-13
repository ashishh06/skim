package com.skim.service;

import com.skim.config.RateLimitProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks total real AI calls served today across all clients, independent of
 * per-IP limits. Protects the shared Gemini free-tier quota from being exhausted
 * even when no single client is individually "abusing" the system.
 */
@Service
public class GlobalUsageService {

    private final RateLimitProperties properties;
    private final AtomicInteger countToday = new AtomicInteger(0);
    private volatile LocalDate currentDay = LocalDate.now();

    public GlobalUsageService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public synchronized boolean tryConsume() {
        resetIfNewDay();
        if (countToday.get() >= properties.getGlobalDailyLimit()) {
            return false;
        }
        countToday.incrementAndGet();
        return true;
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDay)) {
            currentDay = today;
            countToday.set(0);
        }
    }
}
