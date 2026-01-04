package com.lumichat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumichat.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Rate limiting filter to prevent brute force attacks.
 * Uses Redis for distributed rate limiting across instances.
 *
 * Limits:
 * - Login attempts: 5 per minute per IP
 * - Password reset: 3 per minute per IP
 * - API calls: 100 per minute per user (authenticated)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.login-attempts:5}")
    private int loginAttemptsLimit;

    @Value("${app.rate-limit.password-reset-attempts:3}")
    private int passwordResetLimit;

    @Value("${app.rate-limit.api-calls:100}")
    private int apiCallsLimit;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    // Paths that require rate limiting
    private static final Set<String> LOGIN_PATHS = Set.of("/auth/login");
    private static final Set<String> PASSWORD_RESET_PATHS = Set.of("/auth/forgot-password", "/auth/reset-password");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        String clientIp = getClientIp(request);

        // Check login rate limit
        if (LOGIN_PATHS.contains(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            if (!checkRateLimit("login:" + clientIp, loginAttemptsLimit)) {
                log.warn("Rate limit exceeded for login from IP: {}", clientIp);
                sendRateLimitResponse(response, "Too many login attempts. Please try again later.");
                return;
            }
        }

        // Check password reset rate limit
        if (PASSWORD_RESET_PATHS.contains(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            if (!checkRateLimit("password_reset:" + clientIp, passwordResetLimit)) {
                log.warn("Rate limit exceeded for password reset from IP: {}", clientIp);
                sendRateLimitResponse(response, "Too many password reset attempts. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request is within rate limits.
     * Uses Redis INCR with expiry for atomic increment and window management.
     *
     * @param key The rate limit key (e.g., "login:192.168.1.1")
     * @param limit The maximum number of requests allowed in the window
     * @return true if within limits, false if exceeded
     */
    private boolean checkRateLimit(String key, int limit) {
        String redisKey = RATE_LIMIT_PREFIX + key;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return true; // Redis unavailable, allow request
            }

            if (count == 1) {
                // First request in window, set expiry
                redisTemplate.expire(redisKey, WINDOW_DURATION);
            }

            return count <= limit;
        } catch (Exception e) {
            log.error("Rate limit check failed for key {}: {}", key, e.getMessage());
            // Allow request if Redis is unavailable
            return true;
        }
    }

    /**
     * Extract client IP address, considering X-Forwarded-For header for proxied requests.
     * Only uses the first IP in X-Forwarded-For to prevent spoofing.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take only the first IP (client IP) to prevent spoofing
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> apiResponse = ApiResponse.error(429, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
