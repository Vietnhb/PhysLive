package com.example.backend.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Read-only projection of a {@link com.example.backend.entity.User}.
 * Password hash is never exposed. Includes all profile and account-state fields.
 */
@Getter
@lombok.RequiredArgsConstructor
public class UserResponse {

    private final Integer id;
    private final String email;
    private final String fullName;
    private final Integer roleId;
    private final String role;

    /** {@code true} = account is active; {@code false} = disabled by ADMIN. */
    private final boolean enabled;

    /** Legacy / soft-delete flag in DB schema. */
    private final Boolean active;

    /** School / institution this user belongs to. May be {@code null}. */
    private final String institutionId;

    /** UTC timestamp of the most recent successful login. {@code null} if never logged in. */
    private final OffsetDateTime lastLogin;

    /** URL of the user's avatar image. */
    private final String avatarUrl;

    /** Timestamp when the account was created. */
    private final OffsetDateTime createdAt;

    /** Timestamp when the account was last updated. */
    private final OffsetDateTime updatedAt;
    @lombok.Setter
    private java.util.UUID schoolId;
    @lombok.Setter
    private java.util.List<String> permissions = java.util.List.of();
}
