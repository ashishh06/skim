package com.skim.service;

import com.skim.config.RateLimitProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Caps how many requests can be in-flight to Gemini at the same time. Smooths
 * sudden bursts (e.g. several people clicking "summarize" within the same second
 * during a live demo) instead of firing every call at Gemini simultaneously.
 */
@Service
public class ConcurrencyLimiterService {

    private final Semaphore semaphore;

    public ConcurrencyLimiterService(RateLimitProperties properties) {
        this.semaphore = new Semaphore(properties.getMaxConcurrentAiCalls());
    }

    // Waits briefly for a free slot rather than failing instantly, so short bursts
    // queue for a few seconds instead of being rejected outright.
    public boolean tryAcquire() {
        try {
            return semaphore.tryAcquire(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release() {
        semaphore.release();
    }
}
