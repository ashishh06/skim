package com.skim.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * All thresholds are tunable via application.properties (prefix "skim.ratelimit")
 * so limits can be adjusted for a live demo without touching code.
 */
@Component
@ConfigurationProperties(prefix = "skim.ratelimit")
@Data
public class RateLimitProperties {

    private int perIpBurstLimit = 5;       // max requests per minute, per IP
    private int perIpDailyLimit = 30;      // max requests per day, per IP
    private int globalDailyLimit = 250;    // max real AI calls served per day, across everyone
    private int maxConcurrentAiCalls = 3;  // max requests in-flight to Gemini at once
    private int cacheTtlMinutes = 15;      // how long identical requests are served from cache
}
