package com.lumichat.controller;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection dbConnection;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private MinioClient minioClient;

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController(dataSource, redisTemplate, minioClient);
    }

    @Nested
    @DisplayName("Full Health Check")
    class FullHealthCheckTests {

        @Test
        @DisplayName("Should return UP when all dependencies are healthy")
        @SuppressWarnings("unchecked")
        void shouldReturnUpWhenAllHealthy() throws Exception {
            // Database healthy
            when(dataSource.getConnection()).thenReturn(dbConnection);
            when(dbConnection.isValid(anyInt())).thenReturn(true);

            // Redis healthy
            when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
            when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
            when(redisConnection.ping()).thenReturn("PONG");

            // MinIO healthy
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            ResponseEntity<Map<String, Object>> response = healthController.health();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("UP");

            Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");
            assertThat(checks).containsKey("database");
            assertThat(checks).containsKey("redis");
            assertThat(checks).containsKey("minio");

            Map<String, Object> dbCheck = (Map<String, Object>) checks.get("database");
            assertThat(dbCheck.get("status")).isEqualTo("UP");

            verify(dbConnection).close();
        }

        @Test
        @DisplayName("Should return DOWN when database is unhealthy")
        @SuppressWarnings("unchecked")
        void shouldReturnDownWhenDatabaseUnhealthy() throws Exception {
            // Database unhealthy
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

            // Redis healthy
            when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
            when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
            when(redisConnection.ping()).thenReturn("PONG");

            // MinIO healthy
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            ResponseEntity<Map<String, Object>> response = healthController.health();

            assertThat(response.getStatusCode().value()).isEqualTo(503);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("DOWN");

            Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");
            Map<String, Object> dbCheck = (Map<String, Object>) checks.get("database");
            assertThat(dbCheck.get("status")).isEqualTo("DOWN");
            assertThat(dbCheck.get("error")).isNotNull();
        }

        @Test
        @DisplayName("Should return DOWN when Redis is unhealthy")
        @SuppressWarnings("unchecked")
        void shouldReturnDownWhenRedisUnhealthy() throws Exception {
            // Database healthy
            when(dataSource.getConnection()).thenReturn(dbConnection);
            when(dbConnection.isValid(anyInt())).thenReturn(true);

            // Redis unhealthy
            when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
            when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Redis unavailable"));

            // MinIO healthy
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            ResponseEntity<Map<String, Object>> response = healthController.health();

            assertThat(response.getStatusCode().value()).isEqualTo(503);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("DOWN");

            Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");
            Map<String, Object> redisCheck = (Map<String, Object>) checks.get("redis");
            assertThat(redisCheck.get("status")).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should return DOWN when MinIO is unhealthy")
        @SuppressWarnings("unchecked")
        void shouldReturnDownWhenMinioUnhealthy() throws Exception {
            // Database healthy
            when(dataSource.getConnection()).thenReturn(dbConnection);
            when(dbConnection.isValid(anyInt())).thenReturn(true);

            // Redis healthy
            when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
            when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
            when(redisConnection.ping()).thenReturn("PONG");

            // MinIO unhealthy
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("MinIO unavailable"));

            ResponseEntity<Map<String, Object>> response = healthController.health();

            assertThat(response.getStatusCode().value()).isEqualTo(503);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("DOWN");

            Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");
            Map<String, Object> minioCheck = (Map<String, Object>) checks.get("minio");
            assertThat(minioCheck.get("status")).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestamp() throws Exception {
            // Database healthy
            when(dataSource.getConnection()).thenReturn(dbConnection);
            when(dbConnection.isValid(anyInt())).thenReturn(true);

            // Redis healthy
            when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
            when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
            when(redisConnection.ping()).thenReturn("PONG");

            // MinIO healthy
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            long before = System.currentTimeMillis();
            ResponseEntity<Map<String, Object>> response = healthController.health();
            long after = System.currentTimeMillis();

            assertThat(response.getBody()).isNotNull();
            Long timestamp = (Long) response.getBody().get("timestamp");
            assertThat(timestamp).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("Liveness Probe")
    class LivenessProbeTests {

        @Test
        @DisplayName("Should always return UP for liveness")
        void shouldAlwaysReturnUp() {
            Map<String, Object> result = healthController.liveness();

            assertThat(result.get("status")).isEqualTo("UP");
            assertThat(result.get("timestamp")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Readiness Probe")
    class ReadinessProbeTests {

        @Test
        @DisplayName("Should delegate to full health check")
        void shouldDelegateToHealthCheck() throws Exception {
            // Database healthy
            when(dataSource.getConnection()).thenReturn(dbConnection);
            when(dbConnection.isValid(anyInt())).thenReturn(true);

            // Redis healthy
            when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
            when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
            when(redisConnection.ping()).thenReturn("PONG");

            // MinIO healthy
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            ResponseEntity<Map<String, Object>> response = healthController.readiness();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("UP");
            assertThat(response.getBody().get("checks")).isNotNull();
        }
    }
}
