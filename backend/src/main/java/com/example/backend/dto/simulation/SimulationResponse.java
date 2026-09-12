package com.example.backend.dto.simulation;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.entity.SimulationStatus;
import com.example.backend.physics.model.PhysicsArchetype;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simulation admitted by a passed physics validation")
public record SimulationResponse(
        UUID id,
        UUID validationRunId,
        UUID specificationId,
        PhysicsArchetype archetype,
        SimulationStatus status,
        JsonNode initialParameters,
        JsonNode sceneDefinition,
        Instant createdAt,
        Instant updatedAt) {
}
