package com.weekahead.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long accessTokenExpirationMs = 15 * 60 * 1000;

    public JwtService(
            @Value("${JWT_SECRET}") String jwtSecret) {

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET must be configured"
            );
        }

        byte[] secretBytes =
                jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateAccessToken(Long userId, String email) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(
                        new Date(
                                now.toEpochMilli()
                                        + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}