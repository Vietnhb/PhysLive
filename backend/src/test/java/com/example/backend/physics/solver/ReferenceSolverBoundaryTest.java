package com.example.backend.physics.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.reference.ConstantAccelerationReferenceSolver;
import com.example.backend.physics.solver.reference.HorizontalForceReferenceSolver;
import com.example.backend.physics.solver.reference.InclinedPlaneReferenceSolver;
import com.example.backend.physics.solver.reference.ParallelCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.SeriesCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.UniformMotionReferenceSolver;
import com.example.backend.physics.validation.ArchetypeInputValidator;

class ReferenceSolverBoundaryTest {

    private ReferenceSolverRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ReferenceSolverRegistry(
                List.of(new UniformMotionReferenceSolver(), new ConstantAccelerationReferenceSolver(),
                        new HorizontalForceReferenceSolver(), new InclinedPlaneReferenceSolver(),
                        new SeriesCircuitReferenceSolver(), new ParallelCircuitReferenceSolver()),
                new ArchetypeInputValidator(new ArchetypeCatalog()));
    }

    @Test
    void uniformMotionAllowsZeroDuration() {
        SolverResult result = registry.solve(input(PhysicsArchetype.UNIFORM_MOTION,
                Map.of("initialPosition", decimal("7"), "velocity", decimal("-3"), "duration", BigDecimal.ZERO),
                Map.of(), Map.of("initialPosition", "m", "velocity", "m/s", "duration", "s")));
        assertDecimal(result.outputs().get("finalPosition"), "7");
    }

    @Test
    void constantAccelerationHandlesNegativeAcceleration() {
        SolverResult result = registry.solve(input(PhysicsArchetype.CONSTANT_ACCELERATION,
                Map.of("initialVelocity", decimal("10"), "acceleration", decimal("-2"), "duration", decimal("5")),
                Map.of(), Map.of("initialVelocity", "m/s", "acceleration", "m/s2", "duration", "s")));
        assertDecimal(result.outputs().get("finalVelocity"), "0");
        assertDecimal(result.outputs().get("finalPosition"), "25");
    }

    @Test
    void horizontalStaticFrictionKeepsObjectAtRest() {
        SolverResult result = registry.solve(input(PhysicsArchetype.HORIZONTAL_FORCE,
                Map.of("mass", decimal("2"), "appliedForce", decimal("3"),
                        "frictionCoefficient", decimal("0.2"), "gravity", decimal("10"),
                        "duration", decimal("4")),
                Map.of(), Map.of("mass", "kg", "appliedForce", "N", "frictionCoefficient", "1",
                        "gravity", "m/s2", "duration", "s")));
        assertDecimal(result.outputs().get("frictionForce"), "3");
        assertDecimal(result.outputs().get("acceleration"), "0");
        assertDecimal(result.outputs().get("finalPosition"), "0");
    }

    @Test
    void horizontalMotionStopsDuringIntervalWithoutReversing() {
        SolverResult result = registry.solve(input(PhysicsArchetype.HORIZONTAL_FORCE,
                Map.of("mass", decimal("1"), "appliedForce", BigDecimal.ZERO,
                        "frictionCoefficient", decimal("0.5"), "gravity", decimal("10"),
                        "initialVelocity", decimal("10"), "duration", decimal("3")),
                Map.of(), Map.of("mass", "kg", "appliedForce", "N", "frictionCoefficient", "1",
                        "gravity", "m/s2", "initialVelocity", "m/s", "duration", "s")));
        assertDecimal(result.outputs().get("finalVelocity"), "0");
        assertDecimal(result.outputs().get("finalPosition"), "10");
        assertDecimal(result.outputs().get("acceleration"), "0");
    }

    @Test
    void inclinedPlaneCanBeHeldByStaticFriction() {
        SolverResult result = registry.solve(input(PhysicsArchetype.INCLINED_PLANE,
                Map.of("mass", decimal("1"), "angle", decimal("0.2"),
                        "frictionCoefficient", decimal("1"), "gravity", decimal("10"),
                        "duration", decimal("2")),
                Map.of(), Map.of("mass", "kg", "angle", "rad", "frictionCoefficient", "1",
                        "gravity", "m/s2", "duration", "s")));
        assertDecimal(result.outputs().get("finalVelocity"), "0");
        assertDecimal(result.outputs().get("finalPosition"), "0");
        assertDecimal(result.outputs().get("acceleration"), "0");
    }

    @Test
    void seriesCircuitAllowsZeroVoltage() {
        SolverResult result = registry.solve(circuitInput(PhysicsArchetype.SERIES_CIRCUIT, "0", "5", "5"));
        assertDecimal(result.outputs().get("equivalentResistance"), "10");
        assertDecimal(result.outputs().get("totalCurrent"), "0");
        assertThat(result.seriesOutputs().get("voltageDrops")).allMatch(value -> value.signum() == 0);
    }

    @Test
    void parallelCircuitAllowsZeroVoltage() {
        SolverResult result = registry.solve(circuitInput(PhysicsArchetype.PARALLEL_CIRCUIT, "0", "10", "10"));
        assertDecimal(result.outputs().get("equivalentResistance"), "5");
        assertDecimal(result.outputs().get("totalCurrent"), "0");
        assertThat(result.seriesOutputs().get("branchCurrents")).allMatch(value -> value.signum() == 0);
    }

    @Test
    void rejectsInvalidInputUsingSharedPhysicsException() {
        SolverInput invalid = input(PhysicsArchetype.HORIZONTAL_FORCE,
                Map.of("mass", BigDecimal.ZERO, "appliedForce", decimal("1"), "duration", decimal("1")),
                Map.of(), Map.of("mass", "kg", "appliedForce", "N", "duration", "s"));
        assertThatThrownBy(() -> registry.solve(invalid))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("mass");
    }

    @Test
    void rejectsNonPositiveCircuitResistance() {
        SolverInput invalid = circuitInput(PhysicsArchetype.PARALLEL_CIRCUIT, "12", "10", "0");
        assertThatThrownBy(() -> registry.solve(invalid))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("resistance");
    }

    private SolverInput circuitInput(PhysicsArchetype archetype, String voltage, String... resistances) {
        return input(archetype, Map.of("voltage", decimal(voltage)),
                Map.of("resistances", java.util.Arrays.stream(resistances).map(BigDecimal::new).toList()),
                Map.of("voltage", "V", "resistances", "ohm"));
    }

    private SolverInput input(PhysicsArchetype archetype, Map<String, BigDecimal> values,
            Map<String, List<BigDecimal>> seriesValues, Map<String, String> units) {
        return new SolverInput(null, "1.0", archetype, values, seriesValues, units);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private void assertDecimal(BigDecimal actual, String expected) {
        assertThat(actual).isEqualByComparingTo(expected);
    }
}
