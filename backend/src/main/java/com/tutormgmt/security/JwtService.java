package com.tutormgmt.security;

import com.tutormgmt.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Issues and validates the app session JWT (HS256, signed with {@code JWT_SECRET}). */
@Slf4j
@Component
public class JwtService {

    private final SecretKey key;
    private final java.time.Duration ttl;

    public JwtService(AppProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            log.warn("JWT secret is shorter than 32 bytes — acceptable for local dev only.");
            secret = java.util.Arrays.copyOf(secret, 32);
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.ttl = props.jwt().ttl();
    }

    public String issue(UUID userId, String email, String displayName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("name", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** @return the principal, or {@code null} if the token is missing/invalid/expired. */
    public AppPrincipal parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new AppPrincipal(UUID.fromString(c.getSubject()), c.get("email", String.class), c.get("name", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected session token: {}", ex.getMessage());
            return null;
        }
    }

    public java.time.Duration ttl() {
        return ttl;
    }
}
