package com.lumichat.controller;

import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring and load balancer probes.
 * Checks connectivity to all dependencies: Database, Redis, MinIO.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final MinioClient minioClient;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean allHealthy = true;

        // Check database connectivity
        try {
            try (Connection connection = dataSource.getConnection()) {
                connection.isValid(5);
            }
            checks.put("database", Map.of("status", "UP"));
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            checks.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }

        // Check Redis connectivity
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                checks.put("redis", Map.of("status", "UP"));
            } else {
                checks.put("redis", Map.of("status", "DOWN", "error", "Unexpected ping response"));
                allHealthy = false;
            }
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            checks.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }

        // Check MinIO connectivity
        try {
            // Try to check if a known bucket exists (doesn't matter if true/false)
            minioClient.bucketExists(BucketExistsArgs.builder().bucket("lumichat").build());
            checks.put("minio", Map.of("status", "UP"));
        } catch (Exception e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            checks.put("minio", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }

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
     * Simple liveness probe - just returns OK if the application is running.
     * Use this for Kubernetes liveness probes.
     */
    @GetMapping("/health/live")
    public Map<String, Object> liveness() {
        return Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Readiness probe - checks if the application is ready to receive traffic.
     * Use this for Kubernetes readiness probes.
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        // Delegate to full health check
        return health();
    }
}
