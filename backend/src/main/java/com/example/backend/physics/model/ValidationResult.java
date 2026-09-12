package com.example.backend.physics.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ValidationResult(
        PhysicsValidationStatus status,
        SolverResult numericalResult,
        SolverResult referenceResult,
        Map<String, BigDecimal> absoluteErrors,
        Map<String, BigDecimal> relativeErrors,
        Map<String, ComparisonTolerance> tolerances,
        String failureReason) {

    public ValidationResult {
        Objects.requireNonNull(status, "Validation status is required");
        absoluteErrors = immutableCopy(absoluteErrors);
        relativeErrors = immutableCopy(relativeErrors);
        tolerances = immutableCopy(tolerances);
    }

    private static <K, V> Map<K, V> immutableCopy(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
