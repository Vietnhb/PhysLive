package com.example.backend.dto;

import lombok.Getter;

/**
 * Request body for toggling a user's account state.
 * Used by {@code PATCH /api/user/{id}/state} (ADMIN only).
 */
@Getter
public class UpdateAccountStateRequest {
    /** {@code true} to activate the account, {@code false} to disable it. */
    private boolean enabled;
}
