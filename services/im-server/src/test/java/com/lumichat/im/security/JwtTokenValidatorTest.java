package com.lumichat.im.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenValidator Tests")
class JwtTokenValidatorTest {

    // Test secret - must be at least 256 bits for HS256
    private static final String TEST_SECRET = "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";

    private JwtTokenValidator jwtTokenValidator;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtTokenValidator = new JwtTokenValidator(TEST_SECRET);
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(Long userId, String deviceId, String tokenType, Date expiration) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("type", tokenType)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(secretKey);

        if (deviceId != null) {
            builder.claim("deviceId", deviceId);
        }

        return builder.compact();
    }

    private String createValidAccessToken(Long userId, String deviceId) {
        return createToken(userId, deviceId, "access",
                Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
    }

    @Nested
    @DisplayName("Valid Token Tests")
    class ValidTokenTests {

        @Test
        @DisplayName("Should validate token with userId and deviceId")
        void shouldValidateTokenWithUserIdAndDeviceId() {
            // Given
            String token = createValidAccessToken(1L, "device-123");

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.deviceId()).isEqualTo("device-123");
        }

        @Test
        @DisplayName("Should validate token without deviceId")
        void shouldValidateTokenWithoutDeviceId() {
            // Given
            String token = createValidAccessToken(1L, null);

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.deviceId()).isNull();
        }

        @Test
        @DisplayName("Should validate token with large userId")
        void shouldValidateTokenWithLargeUserId() {
            // Given
            Long largeUserId = 9999999999L;
            String token = createValidAccessToken(largeUserId, "device-123");

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(largeUserId);
        }
    }

    @Nested
    @DisplayName("Expired Token Tests")
    class ExpiredTokenTests {

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            String token = createToken(1L, "device-123", "access",
                    Date.from(Instant.now().minus(1, ChronoUnit.HOURS)));

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject token that just expired")
        void shouldRejectTokenThatJustExpired() {
            // Given
            String token = createToken(1L, "device-123", "access",
                    Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Invalid Signature Tests")
    class InvalidSignatureTests {

        @Test
        @DisplayName("Should reject token with wrong secret")
        void shouldRejectTokenWithWrongSecret() {
            // Given - Create token with different secret
            String differentSecret = "different-secret-key-must-be-at-least-256-bits-long-for-hs256";
            SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

            String token = Jwts.builder()
                    .subject("1")
                    .claim("deviceId", "device-123")
                    .claim("type", "access")
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                    .signWith(differentKey)
                    .compact();

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject tampered token")
        void shouldRejectTamperedToken() {
            // Given
            String validToken = createValidAccessToken(1L, "device-123");
            // Tamper with the token by modifying a character
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(tamperedToken);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Token Type Tests")
    class TokenTypeTests {

        @Test
        @DisplayName("Should accept access token type")
        void shouldAcceptAccessTokenType() {
            // Given
            String token = createToken(1L, "device-123", "access",
                    Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should reject refresh token type")
        void shouldRejectRefreshTokenType() {
            // Given
            String token = createToken(1L, "device-123", "refresh",
                    Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject unknown token type")
        void shouldRejectUnknownTokenType() {
            // Given
            String token = createToken(1L, "device-123", "unknown",
                    Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject token without type")
        void shouldRejectTokenWithoutType() {
            // Given
            String token = Jwts.builder()
                    .subject("1")
                    .claim("deviceId", "device-123")
                    // No type claim
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                    .signWith(secretKey)
                    .compact();

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Malformed Token Tests")
    class MalformedTokenTests {

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken("");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken("not.a.valid.jwt.token");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject random string")
        void shouldRejectRandomString() {
            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken("random-string-12345");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject token with invalid base64")
        void shouldRejectTokenWithInvalidBase64() {
            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken("!!!invalid!!!");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("isTokenValid Convenience Method Tests")
    class IsTokenValidTests {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            // Given
            String token = createValidAccessToken(1L, "device-123");

            // When
            boolean result = jwtTokenValidator.isTokenValid(token);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for invalid token")
        void shouldReturnFalseForInvalidToken() {
            // Given
            String token = "invalid-token";

            // When
            boolean result = jwtTokenValidator.isTokenValid(token);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            // Given
            String token = createToken(1L, "device-123", "access",
                    Date.from(Instant.now().minus(1, ChronoUnit.HOURS)));

            // When
            boolean result = jwtTokenValidator.isTokenValid(token);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for refresh token type")
        void shouldReturnFalseForRefreshTokenType() {
            // Given
            String token = createToken(1L, "device-123", "refresh",
                    Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));

            // When
            boolean result = jwtTokenValidator.isTokenValid(token);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle token with special characters in deviceId")
        void shouldHandleTokenWithSpecialCharactersInDeviceId() {
            // Given
            String deviceId = "device-with-special-chars_123!@#";
            String token = createValidAccessToken(1L, deviceId);

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.deviceId()).isEqualTo(deviceId);
        }

        @Test
        @DisplayName("Should handle token with very long deviceId")
        void shouldHandleTokenWithVeryLongDeviceId() {
            // Given
            String longDeviceId = "device-" + "x".repeat(500);
            String token = createValidAccessToken(1L, longDeviceId);

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.deviceId()).isEqualTo(longDeviceId);
        }

        @Test
        @DisplayName("Should handle token with minimum valid expiration")
        void shouldHandleTokenWithMinimumValidExpiration() {
            // Given - Token that expires in 1 second
            String token = createToken(1L, "device-123", "access",
                    Date.from(Instant.now().plus(1, ChronoUnit.SECONDS)));

            // When
            JwtTokenValidator.TokenInfo result = jwtTokenValidator.validateToken(token);

            // Then
            assertThat(result).isNotNull();
        }
    }
}
