package com.example.backend.dto.simulation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ParameterAdjustmentRequest(
        @NotNull UUID simulationId,
        Map<String, BigDecimal> values,
        Map<String, List<BigDecimal>> seriesValues) {

    public CreateSimulationRunRequest toRunRequest() {
        return new CreateSimulationRunRequest(values, seriesValues);
    }
}
