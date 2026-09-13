package com.skim.rateLimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Runs before the controller for every /api/skim/** request and rejects a client
 * (identified by IP) that has exceeded its rate limit, returning 429 immediately
 * without touching the service layer or the AI call at all.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = resolveClientIp(request);
        RateLimitResult result = rateLimiterService.checkAndConsume(clientIp);

        if (!result.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", result.message())));
            return false;
        }
        return true;
    }

    // NOTE: X-Forwarded-For is only trustworthy once this sits behind a real proxy/load
    // balancer that sets it itself — fine for now, but don't trust it blindly in production
    // without a proxy in front that overwrites client-supplied values.
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
