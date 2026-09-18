package com.example.backend.physics.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record SolverInput(
        UUID specificationId,
        String schemaVersion,
        PhysicsArchetype archetype,
        Map<String, BigDecimal> values,
        Map<String, List<BigDecimal>> seriesValues,
        Map<String, String> units,
        JsonNode endCondition) {

    public SolverInput {
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException("Schema version is required");
        }
        Objects.requireNonNull(archetype, "Archetype is required");
        values = immutableCopy(values);
        seriesValues = immutableSeriesCopy(seriesValues);
        units = immutableCopy(units);
    }

    public SolverInput(
            UUID specificationId,
            String schemaVersion,
            PhysicsArchetype archetype,
            Map<String, BigDecimal> values,
            Map<String, List<BigDecimal>> seriesValues,
            Map<String, String> units) {
        this(specificationId, schemaVersion, archetype, values, seriesValues, units, null);
    }

    public SolverInput(
            UUID specificationId,
            String schemaVersion,
            PhysicsArchetype archetype,
            Map<String, BigDecimal> values,
            Map<String, String> units) {
        this(specificationId, schemaVersion, archetype, values, Map.of(), units, null);
    }

    private static <K, V> Map<K, V> immutableCopy(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
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
