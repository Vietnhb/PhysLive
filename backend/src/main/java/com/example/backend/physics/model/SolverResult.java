package com.example.backend.physics.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SolverResult(
        SolverMethod method,
        String solverVersion,
        Map<String, BigDecimal> outputs,
        Map<String, List<BigDecimal>> seriesOutputs,
        List<SimulationFrame> frames) {

    public SolverResult {
        Objects.requireNonNull(method, "Solver method is required");
        if (solverVersion == null || solverVersion.isBlank()) {
            throw new IllegalArgumentException("Solver version is required");
        }
        outputs = outputs == null || outputs.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(outputs));
        seriesOutputs = immutableSeriesCopy(seriesOutputs);
        frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public SolverResult(
            SolverMethod method,
            String solverVersion,
            Map<String, BigDecimal> outputs,
            List<SimulationFrame> frames) {
        this(method, solverVersion, outputs, Map.of(), frames);
    }

    private static Map<String, List<BigDecimal>> immutableSeriesCopy(Map<String, List<BigDecimal>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<BigDecimal>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return Collections.unmodifiableMap(copy);
    }
}
