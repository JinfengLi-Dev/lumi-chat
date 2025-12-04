package com.lumichat.im.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtTokenValidator {

    private final SecretKey secretKey;

    public JwtTokenValidator(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public TokenInfo validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.parseLong(claims.getSubject());
            String deviceId = claims.get("deviceId", String.class);
            String tokenType = claims.get("type", String.class);

            // Only accept access tokens for WebSocket connections
            if (!"access".equals(tokenType)) {
                log.warn("Rejected non-access token for WebSocket connection");
                return null;
            }

            return new TokenInfo(userId, deviceId);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        return validateToken(token) != null;
    }

    public record TokenInfo(Long userId, String deviceId) {}
}
