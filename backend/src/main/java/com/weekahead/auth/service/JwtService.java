package com.weekahead.auth.service;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey
            = Keys.hmacShaKeyFor(
                    "weekahead-development-secret-key-change-this"
                            .getBytes());

    private final long accessTokenExpirationMs = 15 * 60 * 1000;

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
