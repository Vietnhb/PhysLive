package com.example.backend.dto.validation;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.entity.ValidationRunStatus;
import com.example.backend.physics.model.PhysicsArchetype;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persisted result of a numerical/reference physics validation")
public record ValidationRunResponse(
        UUID id,
        UUID specificationId,
        PhysicsArchetype archetype,
        ValidationRunStatus status,
        JsonNode input,
        JsonNode numericalOutput,
        JsonNode referenceOutput,
        JsonNode absoluteErrors,
        JsonNode relativeErrors,
        JsonNode tolerances,
        String schemaVersion,
        String numericalSolverVersion,
        String referenceSolverVersion,
        Instant startedAt,
        Instant completedAt,
        Long executionTimeMs,
        String failureReason) {
}
