package com.agrinexus.service;

import com.agrinexus.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long expirationMs;


    public JwtService(
            @Value("${agrinexus.jwt.secret}")
            String secret,

            @Value("${agrinexus.jwt.expiration-ms:86400000}")
            long expirationMs
    ) {

        if (secret == null || secret.length() < 32) {

            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 characters"
            );
        }

        this.secretKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        this.expirationMs =
                expirationMs;
    }


    /*
     * =========================================================
     * Generate token
     * =========================================================
     */
    public String generateToken(
            User user
    ) {

        Map<String, Object> claims =
                new HashMap<>();

        claims.put(
                "userId",
                user.getId().toString()
        );

        claims.put(
                "role",
                user.getRole().name()
        );

        claims.put(
                "email",
                user.getEmail()
        );


        Date issuedAt =
                new Date();


        Date expiration =
                new Date(
                        issuedAt.getTime()
                                + expirationMs
                );


        return Jwts.builder()

                .claims(
                        claims
                )

                .subject(
                        user.getId().toString()
                )

                .issuedAt(
                        issuedAt
                )

                .expiration(
                        expiration
                )

                .signWith(
                        secretKey
                )

                .compact();
    }


    /*
     * =========================================================
     * Extract user ID
     * =========================================================
     */
    public String extractUserId(
            String token
    ) {

        return extractAllClaims(
                token
        ).getSubject();
    }


    /*
     * =========================================================
     * Extract email
     * =========================================================
     */
    public String extractEmail(
            String token
    ) {

        return extractAllClaims(
                token
        ).get("email", String.class);
    }


    /*
     * =========================================================
     * Extract role
     * =========================================================
     */
    public String extractRole(
            String token
    ) {

        return extractAllClaims(
                token
        ).get("role", String.class);
    }


    /*
     * =========================================================
     * Validate token against user
     * =========================================================
     */
    public boolean isTokenValid(
            String token,
            User user
    ) {

        try {

            String userId =
                    extractUserId(
                            token
                    );


            return userId != null
                    && user.getId() != null
                    && user.getId()
                    .toString()
                    .equals(
                            userId
                    )
                    && !isTokenExpired(
                    token
            );

        } catch (Exception ex) {

            return false;
        }
    }


    /*
     * =========================================================
     * Check expiration
     * =========================================================
     */
    private boolean isTokenExpired(
            String token
    ) {

        Date expiration =
                extractAllClaims(
                        token
                ).getExpiration();


        return expiration.before(
                new Date()
        );
    }


    /*
     * =========================================================
     * Parse claims
     * =========================================================
     */
    private Claims extractAllClaims(
            String token
    ) {

        return Jwts.parser()

                .verifyWith(
                        secretKey
                )

                .build()

                .parseSignedClaims(
                        token
                )

                .getPayload();
    }
}