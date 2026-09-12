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
import com.example.backend.physics.solver.numerical.ConstantAccelerationNumericalSolver;
import com.example.backend.physics.solver.numerical.HorizontalForceNumericalSolver;
import com.example.backend.physics.solver.numerical.InclinedPlaneNumericalSolver;
import com.example.backend.physics.solver.numerical.ParallelCircuitNumericalSolver;
import com.example.backend.physics.solver.numerical.SeriesCircuitNumericalSolver;
import com.example.backend.physics.solver.numerical.UniformMotionNumericalSolver;
import com.example.backend.physics.validation.ArchetypeInputValidator;

class NumericalSolverBoundaryTest {

    private NumericalSolverRegistry registry;

    @BeforeEach
    void setUp() {
        NumericalSolverLimits limits = new NumericalSolverLimits();
        registry = new NumericalSolverRegistry(
                List.of(
                        new UniformMotionNumericalSolver(limits),
                        new ConstantAccelerationNumericalSolver(limits),
                        new HorizontalForceNumericalSolver(limits),
                        new InclinedPlaneNumericalSolver(limits),
                        new SeriesCircuitNumericalSolver(),
                        new ParallelCircuitNumericalSolver()),
                new ArchetypeInputValidator(new ArchetypeCatalog()));
    }

    @Test
    void zeroDurationProducesOnlyInitialFrame() {
        SolverResult result = registry.solve(motionInput("0", "0.01"));

        assertThat(result.frames()).hasSize(1);
        assertThat(result.outputs().get("finalPosition")).isEqualByComparingTo("2");
    }

    @Test
    void finalPartialStepEndsExactlyAtDuration() {
        SolverResult result = registry.solve(motionInput("1", "0.3"));

        assertThat(result.frames()).hasSize(5);
        assertThat(result.frames().get(4).time()).isEqualByComparingTo("1");
        assertThat(result.outputs().get("finalPosition")).isEqualByComparingTo("12");
    }

    @Test
    void rejectsZeroDeltaTime() {
        assertThatThrownBy(() -> registry.solve(motionInput("1", "0")))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("deltaTime");
    }

    @Test
    void rejectsDurationAboveLimit() {
        assertThatThrownBy(() -> registry.solve(motionInput("301", "1")))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("300");
    }

    @Test
    void rejectsRunAboveFrameLimit() {
        assertThatThrownBy(() -> registry.solve(motionInput("20", "0.001")))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("10001");
    }

    @Test
    void zeroVoltageCircuitStillCalculatesEquivalentResistance() {
        SolverInput input = new SolverInput(
                null,
                "1.0",
                PhysicsArchetype.PARALLEL_CIRCUIT,
                Map.of("voltage", BigDecimal.ZERO),
                Map.of("resistances", List.of(new BigDecimal("10"), new BigDecimal("20"))),
                Map.of("voltage", "V", "resistances", "ohm"));

        SolverResult result = registry.solve(input);

        assertThat(result.outputs().get("totalCurrent")).isEqualByComparingTo("0");
        assertThat(result.outputs().get("equivalentResistance").doubleValue())
                .isCloseTo(6.666666666666667, org.assertj.core.data.Offset.offset(1E-14));
    }

    private SolverInput motionInput(String duration, String deltaTime) {
        return new SolverInput(
                null,
                "1.0",
                PhysicsArchetype.UNIFORM_MOTION,
                Map.of(
                        "initialPosition", new BigDecimal("2"),
                        "velocity", new BigDecimal("10"),
                        "duration", new BigDecimal(duration),
                        "deltaTime", new BigDecimal(deltaTime)),
                Map.of(),
                Map.of("initialPosition", "m", "velocity", "m/s", "duration", "s", "deltaTime", "s"));
    }
}
