package com.example.backend.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Read-only projection of a {@link com.example.backend.entity.User}.
 * Password hash is never exposed. Includes all profile and account-state fields.
 */
@Getter
@AllArgsConstructor
public class UserResponse {

    private Integer id;
    private String email;
    private String fullName;
    private Integer roleId;
    private String role;

    /** {@code true} = account is active; {@code false} = disabled by ADMIN. */
    private boolean enabled;

    /** Legacy / soft-delete flag in DB schema. */
    private Boolean active;

    /** School / institution this user belongs to. May be {@code null}. */
    private String institutionId;

    /** UTC timestamp of the most recent successful login. {@code null} if never logged in. */
    private OffsetDateTime lastLogin;
}
