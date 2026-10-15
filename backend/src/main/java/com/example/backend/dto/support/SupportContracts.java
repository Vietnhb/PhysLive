package com.example.backend.dto.support;

import com.example.backend.entity.enums.SupportKind;
import com.example.backend.entity.enums.SupportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Request and response contracts for feedback and support messages. */
public final class SupportContracts {
    private SupportContracts() {
    }

    public record CreateSupportRequest(
            @NotBlank @Size(max = 180) String subject,
            @NotBlank @Size(max = 10000) String content) {
    }

    public record UpdateSupportRequest(
            SupportStatus status,
            @Size(max = 10000) String response) {
    }

    public record SupportView(UUID id, SupportKind kind, Integer senderId, String senderName,
                              String senderEmail, String subject, String content, SupportStatus status,
                              String adminResponse, OffsetDateTime createdAt, Instant respondedAt,
                              String responderName) {
    }
}
