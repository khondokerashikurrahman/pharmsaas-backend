
package com.pharmsaas.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret must not be empty");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 bytes");
        }

        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be greater than zero");
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    // Global token: used by Super Admin.
    public String generateToken(Long userId, String email) {
        return generateTokenInternal(userId, email, null, false);
    }

    // Token for a selected pharmacy.
    public String generateToken(
            Long userId,
            String email,
            Long tenantId) {

        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException(
                    "Tenant ID must be positive");
        }

        return generateTokenInternal(userId, email, tenantId, false);
    }

    // Restricted token used only to select a pharmacy.
    public String generateSelectionToken(
            Long userId,
            String email) {

        return generateTokenInternal(userId, email, null, true);
    }

    private String generateTokenInternal(
            Long userId,
            String email,
            Long tenantId,
            boolean selectionOnly) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "User ID must be positive");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email must not be empty");
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("selectionOnly", selectionOnly)
                .issuedAt(now)
                .expiration(expiry);

        if (tenantId != null) {
            builder.claim("tenantId", tenantId);
        }

        return builder.signWith(secretKey).compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractClaims(token).getSubject());
    }

    public Long extractTenantId(String token) {
        Object value = extractClaims(token).get("tenantId");

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalArgumentException("Invalid tenant ID in token");
    }

    public boolean isSelectionToken(Claims claims) {
        return Boolean.TRUE.equals(
                claims.get("selectionOnly", Boolean.class));
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);

            return claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
