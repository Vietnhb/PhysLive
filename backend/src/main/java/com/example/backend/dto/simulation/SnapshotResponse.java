package com.example.backend.dto.simulation;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persisted replay state at one simulation time")
public record SnapshotResponse(
        UUID id,
        int sequenceNumber,
        BigDecimal simulationTime,
        JsonNode state) {
}
