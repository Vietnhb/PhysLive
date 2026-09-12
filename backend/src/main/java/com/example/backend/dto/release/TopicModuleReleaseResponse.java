package com.example.backend.dto.release;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.physics.model.PhysicsArchetype;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Versioned release binding a topic, schema and solver pair")
public record TopicModuleReleaseResponse(
        UUID id,
        UUID topicId,
        String topicSlug,
        PhysicsArchetype archetype,
        String releaseVersion,
        String schemaVersion,
        String numericalSolverVersion,
        String referenceSolverVersion,
        ModuleReleaseStatus status,
        Integer createdBy,
        Integer approvedBy,
        Integer retiredBy,
        Instant createdAt,
        Instant updatedAt,
        Instant approvedAt,
        Instant retiredAt) {
}
