package com.lumichat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        rateLimitFilter = new RateLimitFilter(redisTemplate, objectMapper);

        // Set default configuration values
        ReflectionTestUtils.setField(rateLimitFilter, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(rateLimitFilter, "loginAttemptsLimit", 5);
        ReflectionTestUtils.setField(rateLimitFilter, "passwordResetLimit", 3);
        ReflectionTestUtils.setField(rateLimitFilter, "apiCallsLimit", 100);

        // Use lenient() to avoid UnnecessaryStubbingException for tests that skip rate limiting
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Login Rate Limiting")
    class LoginRateLimitTests {

        @Test
        @DisplayName("Should allow login within rate limit")
        void shouldAllowLoginWithinLimit() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(valueOperations.increment(anyString())).thenReturn(1L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(redisTemplate).expire(anyString(), eq(Duration.ofMinutes(1)));
        }

        @Test
        @DisplayName("Should allow up to 5 login attempts")
        void shouldAllowFiveLoginAttempts() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(valueOperations.increment(anyString())).thenReturn(5L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should block 6th login attempt")
        void shouldBlockSixthLoginAttempt() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(valueOperations.increment(anyString())).thenReturn(6L);

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(printWriter);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

            String responseBody = stringWriter.toString();
            assertThat(responseBody).contains("Too many login attempts");
        }

        @Test
        @DisplayName("Should use X-Forwarded-For header for IP")
        void shouldUseXForwardedForHeader() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
            when(valueOperations.increment(anyString())).thenReturn(1L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(valueOperations).increment("rate_limit:login:10.0.0.1");
        }
    }

    @Nested
    @DisplayName("Password Reset Rate Limiting")
    class PasswordResetRateLimitTests {

        @Test
        @DisplayName("Should allow password reset within rate limit")
        void shouldAllowPasswordResetWithinLimit() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/forgot-password");
            when(request.getMethod()).thenReturn("POST");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(valueOperations.increment(anyString())).thenReturn(1L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should block 4th password reset attempt")
        void shouldBlockFourthPasswordResetAttempt() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/forgot-password");
            when(request.getMethod()).thenReturn("POST");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(valueOperations.increment(anyString())).thenReturn(4L);

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(printWriter);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    @Nested
    @DisplayName("Rate Limit Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("Should skip rate limiting when disabled")
        void shouldSkipWhenDisabled() throws Exception {
            ReflectionTestUtils.setField(rateLimitFilter, "rateLimitEnabled", false);

            when(request.getServletPath()).thenReturn("/auth/login");
            when(request.getMethod()).thenReturn("POST");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("Should allow request when Redis is unavailable")
        void shouldAllowWhenRedisUnavailable() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis unavailable"));

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not rate limit GET requests to login path")
        void shouldNotRateLimitGetRequests() throws Exception {
            when(request.getServletPath()).thenReturn("/auth/login");
            when(request.getMethod()).thenReturn("GET");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("Should not rate limit non-auth paths")
        void shouldNotRateLimitNonAuthPaths() throws Exception {
            when(request.getServletPath()).thenReturn("/users/me");
            when(request.getMethod()).thenReturn("GET");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(redisTemplate, never()).opsForValue();
        }
    }
}
