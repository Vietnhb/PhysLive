package com.example.backend.dto.library;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.entity.enums.LibraryModerationStatus;

import jakarta.validation.constraints.Size;

public final class LibraryModerationContracts {
    private LibraryModerationContracts() { }

    public record Request(LibraryModerationStatus status, @Size(max = 4000) String comment) { }

    public record Audit(UUID id, UUID itemId, String reviewerEmail,
            LibraryModerationStatus fromStatus, LibraryModerationStatus toStatus,
            String comment, Instant decidedAt) { }
}
