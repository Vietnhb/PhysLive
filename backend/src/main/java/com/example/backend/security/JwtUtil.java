package com.example.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Stateless JWT utility – generates and validates signed JWTs.
 * Secret and expiry are injected from application.properties / env.
 */
@Component
@Slf4j
public class JwtUtil {

    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret:}") String configuredSecret,
            @Value("${jwt.expiration:86400000}") long expiration,
            Environment environment) {
        this.expiration = expiration;
        this.secretKey = resolveSecretKey(configuredSecret, environment);
    }


    // ------------------------------------------------------------------ //
    // Token generation
    // ------------------------------------------------------------------ //

    /**
     * Creates a signed JWT containing the user's email (subject) and role claim.
     *
     * @param email user email – used as JWT subject
     * @param role  role name stored as a custom claim
     * @return compact serialised JWT string
     */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    // ------------------------------------------------------------------ //
    // Token inspection
    // ------------------------------------------------------------------ //

    /**
     * Parses and returns all claims from a valid token.
     * Throws {@link JwtException} if the token is malformed, expired, or has an invalid signature.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the email (subject) from a token without throwing on expiry.
     * Validates fully via {@link #extractClaims(String)}.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Returns {@code true} if the token can be parsed and has not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey resolveSecretKey(String configuredSecret, Environment environment) {
        if (StringUtils.hasText(configuredSecret)) {
            byte[] bytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < MINIMUM_SECRET_BYTES) {
                throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes.");
            }
            return Keys.hmacShaKeyFor(bytes);
        }

        if (environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            throw new IllegalStateException("JWT_SECRET is required in production.");
        }

        byte[] generatedSecret = new byte[MINIMUM_SECRET_BYTES];
        new SecureRandom().nextBytes(generatedSecret);
        log.warn("JWT_SECRET is not configured; using an ephemeral development key for this process.");
        return Keys.hmacShaKeyFor(generatedSecret);
    }
}
