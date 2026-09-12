package com.example.backend.dto.simulation;

import java.time.Instant;
import java.util.UUID;

import com.example.backend.entity.SimulationRunStatus;
import com.example.backend.physics.model.PhysicsArchetype;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One immutable numerical execution of a simulation")
public record SimulationRunResponse(
        UUID id,
        UUID simulationId,
        PhysicsArchetype archetype,
        SimulationRunStatus status,
        String solverVersion,
        JsonNode inputParameters,
        JsonNode outputPayload,
        int frameCount,
        Instant startedAt,
        Instant completedAt,
        Long executionTimeMs,
        String errorMessage) {
}
