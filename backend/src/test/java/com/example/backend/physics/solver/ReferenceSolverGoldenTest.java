package com.example.backend.physics.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.reference.ConstantAccelerationReferenceSolver;
import com.example.backend.physics.solver.reference.HorizontalForceReferenceSolver;
import com.example.backend.physics.solver.reference.InclinedPlaneReferenceSolver;
import com.example.backend.physics.solver.reference.ParallelCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.SeriesCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.UniformMotionReferenceSolver;
import com.example.backend.physics.validation.ArchetypeInputValidator;

class ReferenceSolverGoldenTest {

    private ReferenceSolverRegistry registry;

    @BeforeEach
    void setUp() {
        registry = registry();
    }

    @Test
    void solvesUniformMotionByClosedFormFormula() {
        SolverResult result = registry.solve(input(
                PhysicsArchetype.UNIFORM_MOTION,
                Map.of("initialPosition", decimal("2"), "velocity", decimal("10"), "duration", decimal("5")),
                Map.of(),
                Map.of("initialPosition", "m", "velocity", "m/s", "duration", "s")));

        assertThat(result.method()).isEqualTo(SolverMethod.REFERENCE);
        assertThat(result.solverVersion()).isEqualTo("uniform-motion-reference-v1");
        assertDecimal(result.outputs().get("finalPosition"), "52");
        assertThat(result.frames()).isEmpty();
    }

    @Test
    void solvesConstantAccelerationByClosedFormFormula() {
        SolverResult result = registry.solve(input(
                PhysicsArchetype.CONSTANT_ACCELERATION,
                Map.of("initialPosition", decimal("1"), "initialVelocity", decimal("3"),
                        "acceleration", decimal("2"), "duration", decimal("4")),
                Map.of(),
                Map.of("initialPosition", "m", "initialVelocity", "m/s", "acceleration", "m/s2",
                        "duration", "s")));

        assertDecimal(result.outputs().get("finalPosition"), "29");
        assertDecimal(result.outputs().get("finalVelocity"), "11");
    }

    @Test
    void solvesHorizontalForceWithFrictionByClosedFormFormula() {
        SolverResult result = registry.solve(input(
                PhysicsArchetype.HORIZONTAL_FORCE,
                Map.of("mass", decimal("2"), "appliedForce", decimal("10"),
                        "frictionCoefficient", decimal("0.1"), "gravity", decimal("10"),
                        "duration", decimal("2")),
                Map.of(),
                Map.of("mass", "kg", "appliedForce", "N", "frictionCoefficient", "1",
                        "gravity", "m/s2", "duration", "s")));

        assertDecimal(result.outputs().get("normalForce"), "20");
        assertDecimal(result.outputs().get("frictionForce"), "2");
        assertDecimal(result.outputs().get("acceleration"), "4");
        assertDecimal(result.outputs().get("finalVelocity"), "8");
        assertDecimal(result.outputs().get("finalPosition"), "8");
    }

    @Test
    void solvesInclinedPlaneWithoutFrictionByClosedFormFormula() {
        SolverResult result = registry.solve(input(
                PhysicsArchetype.INCLINED_PLANE,
                Map.of("mass", decimal("2"), "angle", decimal("0.5235987755982988"),
                        "gravity", decimal("9.80665"), "duration", decimal("2")),
                Map.of(),
                Map.of("mass", "kg", "angle", "rad", "gravity", "m/s2", "duration", "s")));

        assertThat(result.outputs().get("acceleration").doubleValue()).isCloseTo(4.903325, within(1E-10));
        assertThat(result.outputs().get("finalVelocity").doubleValue()).isCloseTo(9.80665, within(1E-9));
        assertThat(result.outputs().get("finalPosition").doubleValue()).isCloseTo(9.80665, within(1E-9));
    }

    @Test
    void solvesSeriesCircuitByOhmsLaw() {
        SolverResult result = registry.solve(circuitInput(PhysicsArchetype.SERIES_CIRCUIT, "12", "10", "20"));

        assertDecimal(result.outputs().get("equivalentResistance"), "30");
        assertDecimal(result.outputs().get("totalCurrent"), "0.4");
        assertThat(result.seriesOutputs().get("voltageDrops"))
                .satisfiesExactly(value -> assertDecimal(value, "4"), value -> assertDecimal(value, "8"));
    }

    @Test
    void solvesParallelCircuitByConductanceFormula() {
        SolverResult result = registry.solve(circuitInput(PhysicsArchetype.PARALLEL_CIRCUIT, "12", "10", "20"));

        assertThat(result.outputs().get("equivalentResistance").doubleValue())
                .isCloseTo(6.666666666666667, within(1E-14));
        assertDecimal(result.outputs().get("totalCurrent"), "1.8");
        assertThat(result.seriesOutputs().get("branchCurrents"))
                .satisfiesExactly(value -> assertDecimal(value, "1.2"), value -> assertDecimal(value, "0.6"));
    }

    private ReferenceSolverRegistry registry() {
        return new ReferenceSolverRegistry(
                List.of(
                        new UniformMotionReferenceSolver(),
                        new ConstantAccelerationReferenceSolver(),
                        new HorizontalForceReferenceSolver(),
                        new InclinedPlaneReferenceSolver(),
                        new SeriesCircuitReferenceSolver(),
                        new ParallelCircuitReferenceSolver()),
                new ArchetypeInputValidator(new ArchetypeCatalog()));
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

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
