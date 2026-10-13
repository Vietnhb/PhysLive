package com.example.backend.dto.assignment;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentActionLogRequest(
        @NotNull UUID assignmentId,
        @NotBlank @Size(max = 64) String action,
        Map<String, Object> payload) {
}
