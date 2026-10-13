package com.example.backend.security;

import com.example.backend.exception.ApiException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Shared password validation, including BCrypt's 72-byte input limit. */
public final class PasswordPolicy {
    private PasswordPolicy() { }

    public static boolean matches(String raw, String stored, PasswordEncoder encoder) {
        if (raw == null || stored == null) return false;
        return stored.startsWith("$2") ? encoder.matches(raw, stored) : raw.equals(stored);
    }

    public static void requireValid(String password) {
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 120)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain 8 to 120 characters");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password exceeds BCrypt's 72-byte UTF-8 limit");
    }
}
