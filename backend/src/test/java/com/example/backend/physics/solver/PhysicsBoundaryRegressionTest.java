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
import com.example.backend.physics.solver.numerical.ConstantAccelerationNumericalSolver;
import com.example.backend.physics.solver.numerical.HorizontalForceNumericalSolver;
import com.example.backend.physics.solver.numerical.InclinedPlaneNumericalSolver;
import com.example.backend.physics.solver.numerical.ParallelCircuitNumericalSolver;
import com.example.backend.physics.solver.numerical.SeriesCircuitNumericalSolver;
import com.example.backend.physics.solver.numerical.UniformMotionNumericalSolver;
import com.example.backend.physics.solver.reference.ConstantAccelerationReferenceSolver;
import com.example.backend.physics.solver.reference.HorizontalForceReferenceSolver;
import com.example.backend.physics.solver.reference.InclinedPlaneReferenceSolver;
import com.example.backend.physics.solver.reference.ParallelCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.SeriesCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.UniformMotionReferenceSolver;
import com.example.backend.physics.validation.ArchetypeInputValidator;

class PhysicsBoundaryRegressionTest {

    private NumericalSolverRegistry numerical;
    private ReferenceSolverRegistry reference;

    @BeforeEach
    void setUp() {
        ArchetypeCatalog catalog = new ArchetypeCatalog();
        ArchetypeInputValidator numericalValidator = new ArchetypeInputValidator(catalog);
        ArchetypeInputValidator referenceValidator = new ArchetypeInputValidator(catalog);
        NumericalSolverLimits limits = new NumericalSolverLimits();
        numerical = new NumericalSolverRegistry(List.of(
                new UniformMotionNumericalSolver(limits),
                new ConstantAccelerationNumericalSolver(limits),
                new HorizontalForceNumericalSolver(limits),
                new InclinedPlaneNumericalSolver(limits),
                new SeriesCircuitNumericalSolver(),
                new ParallelCircuitNumericalSolver()), numericalValidator);
        reference = new ReferenceSolverRegistry(List.of(
                new UniformMotionReferenceSolver(),
                new ConstantAccelerationReferenceSolver(),
                new HorizontalForceReferenceSolver(),
                new InclinedPlaneReferenceSolver(),
                new SeriesCircuitReferenceSolver(),
                new ParallelCircuitReferenceSolver()), referenceValidator);
    }

    @Test
    void bothSolverFamiliesRejectNegativeDuration() {
        SolverInput input = motionInput("-0.001", "0.01", "10");

        assertRejectedByBoth(input, "duration");
    }

    @Test
    void numericalDynamicsRejectsZeroAndNegativeMass() {
        assertThatThrownBy(() -> numerical.solve(horizontalInput("0")))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("mass");
        assertThatThrownBy(() -> numerical.solve(horizontalInput("-1")))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("mass");
    }

    @Test
    void bothSolverFamiliesRejectAnglesOutsideHalfOpenPhysicalRange() {
        assertRejectedByBoth(inclinedInput("-0.0001"), "angle");
        assertRejectedByBoth(inclinedInput("1.5707963267948966"), "angle");
    }

    @Test
    void bothSolverFamiliesRejectEmptyResistanceList() {
        assertRejectedByBoth(circuitInput(PhysicsArchetype.SERIES_CIRCUIT, List.of()), "empty");
    }

    @Test
    void bothSolverFamiliesRejectZeroAndNegativeResistance() {
        assertRejectedByBoth(circuitInput(PhysicsArchetype.PARALLEL_CIRCUIT,
                List.of(decimal("10"), BigDecimal.ZERO)), "greater than zero");
        assertRejectedByBoth(circuitInput(PhysicsArchetype.SERIES_CIRCUIT,
                List.of(decimal("10"), decimal("-2"))), "greater than zero");
    }

    @Test
    void veryLargeFiniteMotionValuesRemainDeterministic() {
        SolverInput input = motionInput("300", "300", "1E100");

        assertThat(numerical.solve(input).outputs().get("finalPosition"))
                .isEqualByComparingTo("3E102");
        assertThat(reference.solve(input).outputs().get("finalPosition"))
                .isEqualByComparingTo("3E102");
    }

    private void assertRejectedByBoth(SolverInput input, String message) {
        assertThatThrownBy(() -> numerical.solve(input))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining(message);
        assertThatThrownBy(() -> reference.solve(input))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining(message);
    }

    private SolverInput motionInput(String duration, String deltaTime, String velocity) {
        return input(PhysicsArchetype.UNIFORM_MOTION,
                Map.of("velocity", decimal(velocity), "duration", decimal(duration),
                        "deltaTime", decimal(deltaTime)),
                Map.of(), Map.of("velocity", "m/s", "duration", "s", "deltaTime", "s"));
    }

    private SolverInput horizontalInput(String mass) {
        return input(PhysicsArchetype.HORIZONTAL_FORCE,
                Map.of("mass", decimal(mass), "appliedForce", decimal("10"),
                        "duration", decimal("1"), "deltaTime", decimal("0.1")),
                Map.of(), Map.of("mass", "kg", "appliedForce", "N", "duration", "s",
                        "deltaTime", "s"));
    }

    private SolverInput inclinedInput(String angle) {
        return input(PhysicsArchetype.INCLINED_PLANE,
                Map.of("mass", decimal("1"), "angle", decimal(angle), "duration", decimal("1"),
                        "deltaTime", decimal("0.1")),
                Map.of(), Map.of("mass", "kg", "angle", "rad", "duration", "s",
                        "deltaTime", "s"));
    }

    private SolverInput circuitInput(PhysicsArchetype archetype, List<BigDecimal> resistances) {
        return input(archetype, Map.of("voltage", decimal("12")),
                Map.of("resistances", resistances), Map.of("voltage", "V", "resistances", "ohm"));
    }

    private SolverInput input(PhysicsArchetype archetype, Map<String, BigDecimal> values,
            Map<String, List<BigDecimal>> series, Map<String, String> units) {
        return new SolverInput(null, "1.0", archetype, values, series, units);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
