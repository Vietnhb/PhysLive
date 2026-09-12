package com.example.backend.physics.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;

class ArchetypeInputValidatorTest {

    private final ArchetypeInputValidator validator = new ArchetypeInputValidator(new ArchetypeCatalog());

    @ParameterizedTest
    @MethodSource("validInputs")
    void acceptsValidInputForEveryArchetype(SolverInput input) {
        assertThat(validator.validate(input)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void reportsClearBlockersForInvalidInput(SolverInput input, String expectedBlocker) {
        assertThat(validator.validate(input)).anyMatch(blocker -> blocker.contains(expectedBlocker));
    }

    private static Stream<Arguments> validInputs() {
        return Stream.of(
                Arguments.of(input(
                        PhysicsArchetype.UNIFORM_MOTION,
                        Map.of("velocity", decimal("10"), "duration", decimal("5")),
                        Map.of(),
                        Map.of("velocity", "m/s", "duration", "s"))),
                Arguments.of(input(
                        PhysicsArchetype.CONSTANT_ACCELERATION,
                        Map.of("acceleration", decimal("2"), "duration", decimal("5")),
                        Map.of(),
                        Map.of("acceleration", "m/s2", "duration", "s"))),
                Arguments.of(input(
                        PhysicsArchetype.HORIZONTAL_FORCE,
                        Map.of("mass", decimal("2"), "appliedForce", decimal("10"), "duration", decimal("3")),
                        Map.of(),
                        Map.of("mass", "kg", "appliedForce", "N", "duration", "s"))),
                Arguments.of(input(
                        PhysicsArchetype.INCLINED_PLANE,
                        Map.of("mass", decimal("2"), "angle", decimal("0.5"), "duration", decimal("3")),
                        Map.of(),
                        Map.of("mass", "kg", "angle", "rad", "duration", "s"))),
                Arguments.of(input(
                        PhysicsArchetype.SERIES_CIRCUIT,
                        Map.of("voltage", decimal("12")),
                        Map.of("resistances", List.of(decimal("10"), decimal("20"))),
                        Map.of("voltage", "V", "resistances", "ohm"))),
                Arguments.of(input(
                        PhysicsArchetype.PARALLEL_CIRCUIT,
                        Map.of("voltage", decimal("12")),
                        Map.of("resistances", List.of(decimal("10"), decimal("20"))),
                        Map.of("voltage", "V", "resistances", "ohm"))));
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(input(
                        PhysicsArchetype.UNIFORM_MOTION,
                        Map.of("duration", decimal("5")),
                        Map.of(),
                        Map.of("duration", "s")), "velocity"),
                Arguments.of(input(
                        PhysicsArchetype.CONSTANT_ACCELERATION,
                        Map.of("acceleration", decimal("2"), "duration", decimal("-1")),
                        Map.of(),
                        Map.of("acceleration", "m/s2", "duration", "s")), "duration"),
                Arguments.of(input(
                        PhysicsArchetype.HORIZONTAL_FORCE,
                        Map.of("mass", BigDecimal.ZERO, "appliedForce", decimal("10"), "duration", decimal("3")),
                        Map.of(),
                        Map.of("mass", "kg", "appliedForce", "N", "duration", "s")), "mass"),
                Arguments.of(input(
                        PhysicsArchetype.INCLINED_PLANE,
                        Map.of("mass", decimal("2"), "angle", decimal("1.5707963267948966"),
                                "duration", decimal("3")),
                        Map.of(),
                        Map.of("mass", "kg", "angle", "rad", "duration", "s")), "angle"),
                Arguments.of(input(
                        PhysicsArchetype.SERIES_CIRCUIT,
                        Map.of("voltage", decimal("12")),
                        Map.of("resistances", List.of(decimal("10"))),
                        Map.of("voltage", "V", "resistances", "ohm")), "at least two"),
                Arguments.of(input(
                        PhysicsArchetype.PARALLEL_CIRCUIT,
                        Map.of("voltage", decimal("12")),
                        Map.of("resistances", List.of(decimal("10"), BigDecimal.ZERO)),
                        Map.of("voltage", "V", "resistances", "ohm")), "greater than zero"));
    }

    private static SolverInput input(
            PhysicsArchetype archetype,
            Map<String, BigDecimal> values,
            Map<String, List<BigDecimal>> seriesValues,
            Map<String, String> units) {
        return new SolverInput(null, "1.0", archetype, values, seriesValues, units);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
