package com.skim.service;

import com.skim.config.RateLimitProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches AI responses for identical (content, operation, tone) combinations for a
 * short TTL. Cuts quota usage and gives instant results when multiple people test
 * the same sample text (common during demos) — a cache hit never touches Gemini
 * or counts against the global/per-IP limits.
 */
@Service
public class ResponseCacheService {

    private static final int MAX_ENTRIES_BEFORE_CLEANUP = 200;

    private final RateLimitProperties properties;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public ResponseCacheService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            cache.remove(key);
            return null;
        }
        return entry.value;
    }

    public void put(String key, String value) {
        purgeExpiredIfLarge();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(properties.getCacheTtlMinutes()));
        cache.put(key, new CacheEntry(value, expiresAt));
    }

    public static String buildKey(String content, String operation, String tone) {
        String raw = operation + "|" + (tone == null ? "" : tone) + "|" + content;
        return sha256(raw);
    }

    private void purgeExpiredIfLarge() {
        if (cache.size() < MAX_ENTRIES_BEFORE_CLEANUP) {
            return;
        }
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record CacheEntry(String value, Instant expiresAt) {
    }
}
