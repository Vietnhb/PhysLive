package com.example.backend.physics.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.dto.simulation.CreateSimulationRunRequest;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.validation.ArchetypeInputValidator;
import com.example.backend.physics.solver.NumericalSolverLimits;

class SimulationParameterPolicyTest {

    private SimulationParameterPolicy policy;

    @BeforeEach
    void setUp() {
        ArchetypeCatalog catalog = new ArchetypeCatalog();
        policy = new SimulationParameterPolicy(
                catalog, new ArchetypeInputValidator(catalog), new NumericalSolverLimits());
    }

    @Test
    void mergesAllowedSliderValuesWithoutChangingIdentityOrUnits() {
        SolverInput base = uniformInput();

        SolverInput merged = policy.apply(base,
                new CreateSimulationRunRequest(Map.of("velocity", decimal("5")), Map.of()));

        assertThat(merged.archetype()).isEqualTo(PhysicsArchetype.UNIFORM_MOTION);
        assertThat(merged.schemaVersion()).isEqualTo("1.0");
        assertThat(merged.values()).containsEntry("velocity", decimal("5"))
                .containsEntry("duration", decimal("3"));
        assertThat(merged.units()).containsEntry("velocity", "m/s");
        assertThat(base.values().get("velocity")).isEqualByComparingTo("2");
    }

    @Test
    void allowsValidatedSeriesOverrideForCircuit() {
        SolverInput base = new SolverInput(null, "1.0", PhysicsArchetype.SERIES_CIRCUIT,
                Map.of("voltage", decimal("12")),
                Map.of("resistances", List.of(decimal("10"), decimal("20"))),
                Map.of("voltage", "V", "resistances", "ohm"));

        SolverInput merged = policy.apply(base,
                new CreateSimulationRunRequest(Map.of(),
                        Map.of("resistances", List.of(decimal("5"), decimal("15")))));

        assertThat(merged.seriesValues().get("resistances"))
                .containsExactly(decimal("5"), decimal("15"));
    }

    @Test
    void rejectsUnknownOrWrongCardinalityOverride() {
        assertThatThrownBy(() -> policy.apply(uniformInput(),
                new CreateSimulationRunRequest(Map.of("archetype", BigDecimal.ONE), Map.of())))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("not allowed");
        assertThatThrownBy(() -> policy.apply(uniformInput(),
                new CreateSimulationRunRequest(Map.of(), Map.of("velocity", List.of(BigDecimal.ONE)))))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("cardinality");
    }

    @Test
    void rejectsOverrideOutsidePhysicalLimits() {
        assertThatThrownBy(() -> policy.apply(uniformInput(),
                new CreateSimulationRunRequest(Map.of("deltaTime", BigDecimal.ZERO), Map.of())))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("deltaTime");
    }

    @Test
    void rejectsDurationAndDeltaTimeThatWouldCreateTooManyFrames() {
        assertThatThrownBy(() -> policy.apply(uniformInput(),
                new CreateSimulationRunRequest(
                        Map.of("duration", decimal("300"), "deltaTime", decimal("0.001")), Map.of())))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("frames");
    }

    private SolverInput uniformInput() {
        return new SolverInput(null, "1.0", PhysicsArchetype.UNIFORM_MOTION,
                Map.of("velocity", decimal("2"), "duration", decimal("3")),
                Map.of("velocity", "m/s", "duration", "s"));
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
