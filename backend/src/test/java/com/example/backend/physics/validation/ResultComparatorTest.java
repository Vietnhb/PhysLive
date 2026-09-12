package com.example.backend.physics.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.PhysicsValidationStatus;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.model.ValidationResult;

class ResultComparatorTest {

    private ResultComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new ResultComparator(new ArchetypeCatalog(), new ValidationTolerancePolicy());
    }

    @Test
    void passesWhenAbsoluteErrorIsWithinTolerance() {
        ValidationResult result = comparator.compare(PhysicsArchetype.UNIFORM_MOTION,
                scalarResult(SolverMethod.NUMERICAL, "finalPosition", "10.0000000005"),
                scalarResult(SolverMethod.REFERENCE, "finalPosition", "10"));

        assertThat(result.status()).isEqualTo(PhysicsValidationStatus.PASSED);
        assertThat(result.absoluteErrors().get("finalPosition")).isEqualByComparingTo("5E-10");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void passesWhenRelativeErrorIsWithinToleranceEvenIfAbsoluteIsNot() {
        ValidationResult result = comparator.compare(PhysicsArchetype.UNIFORM_MOTION,
                scalarResult(SolverMethod.NUMERICAL, "finalPosition", "1000000000.5"),
                scalarResult(SolverMethod.REFERENCE, "finalPosition", "1000000000"));

        assertThat(result.status()).isEqualTo(PhysicsValidationStatus.PASSED);
        assertThat(result.absoluteErrors().get("finalPosition")).isEqualByComparingTo("0.5");
        assertThat(result.relativeErrors().get("finalPosition")).isEqualByComparingTo("5E-10");
    }

    @Test
    void failsWhenBothAbsoluteAndRelativeErrorsExceedTolerance() {
        ValidationResult result = comparator.compare(PhysicsArchetype.UNIFORM_MOTION,
                scalarResult(SolverMethod.NUMERICAL, "finalPosition", "10.01"),
                scalarResult(SolverMethod.REFERENCE, "finalPosition", "10"));

        assertThat(result.status()).isEqualTo(PhysicsValidationStatus.FAILED);
        assertThat(result.failureReason()).contains("finalPosition");
        assertThat(result.tolerances().get("finalPosition").absolute()).isEqualByComparingTo("1E-9");
    }

    @Test
    void comparesEveryElementOfRequiredSeriesOutput() {
        SolverResult numerical = new SolverResult(SolverMethod.NUMERICAL, "n-v1",
                Map.of("equivalentResistance", decimal("30"), "totalCurrent", decimal("0.4")),
                Map.of("voltageDrops", List.of(decimal("4"), decimal("8.1"))), List.of());
        SolverResult reference = new SolverResult(SolverMethod.REFERENCE, "r-v1",
                Map.of("equivalentResistance", decimal("30"), "totalCurrent", decimal("0.4")),
                Map.of("voltageDrops", List.of(decimal("4"), decimal("8"))), List.of());

        ValidationResult result = comparator.compare(PhysicsArchetype.SERIES_CIRCUIT, numerical, reference);

        assertThat(result.status()).isEqualTo(PhysicsValidationStatus.FAILED);
        assertThat(result.absoluteErrors()).containsKeys(
                "equivalentResistance", "totalCurrent", "voltageDrops[0]", "voltageDrops[1]");
        assertThat(result.failureReason()).contains("voltageDrops[1]");
    }

    @Test
    void failsWhenARequiredOutputIsMissing() {
        SolverResult numerical = new SolverResult(SolverMethod.NUMERICAL, "n-v1", Map.of(), List.of());
        SolverResult reference = scalarResult(SolverMethod.REFERENCE, "finalPosition", "10");

        ValidationResult result = comparator.compare(PhysicsArchetype.UNIFORM_MOTION, numerical, reference);

        assertThat(result.status()).isEqualTo(PhysicsValidationStatus.FAILED);
        assertThat(result.failureReason()).contains("Missing required output");
    }

    private SolverResult scalarResult(SolverMethod method, String key, String value) {
        return new SolverResult(method, method.name().toLowerCase() + "-v1",
                Map.of(key, decimal(value)), List.of());
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
