package com.lumichat.im.controller;

import com.lumichat.im.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint for the IM server.
 * Checks Redis connectivity (required for message routing).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final StringRedisTemplate redisTemplate;
    private final SessionManager sessionManager;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean allHealthy = true;

        // Check Redis connectivity
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                checks.put("redis", Map.of("status", "DOWN", "error", "No connection factory"));
                allHealthy = false;
            } else {
                String pong = connectionFactory.getConnection().ping();
                if ("PONG".equalsIgnoreCase(pong)) {
                    checks.put("redis", Map.of("status", "UP"));
                } else {
                    checks.put("redis", Map.of("status", "DOWN", "error", "Unexpected ping response"));
                    allHealthy = false;
                }
            }
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            checks.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }

        // WebSocket server status (always up if this endpoint responds)
        checks.put("websocket", Map.of(
            "status", "UP",
            "activeSessions", sessionManager.getOnlineUserCount()
        ));

        result.put("status", allHealthy ? "UP" : "DOWN");
        result.put("timestamp", System.currentTimeMillis());
        result.put("checks", checks);

        if (allHealthy) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(503).body(result);
        }
    }

    /**
     * Simple liveness probe.
     */
    @GetMapping("/health/live")
    public Map<String, Object> liveness() {
        return Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Readiness probe - checks dependencies.
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        return health();
    }

    /**
     * Get current session statistics.
     */
    @GetMapping("/health/sessions")
    public Map<String, Object> sessions() {
        return Map.of(
            "onlineUsers", sessionManager.getOnlineUserCount(),
            "activeSessions", sessionManager.getAllSessions().size(),
            "timestamp", System.currentTimeMillis()
        );
    }
}
