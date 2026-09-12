package com.example.backend.dto.release;

import java.util.UUID;

import com.example.backend.physics.model.PhysicsArchetype;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Version references for a new immutable draft module release")
public record CreateTopicModuleReleaseRequest(
        UUID topicId,
        String releaseVersion,
        String schemaVersion,
        PhysicsArchetype archetype) {
}
