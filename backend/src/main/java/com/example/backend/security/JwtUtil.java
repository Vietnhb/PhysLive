package com.example.backend.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Stateless JWT utility – generates and validates signed JWTs.
 * Secret and expiry are injected from application.properties / env.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

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
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
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
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
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
}
