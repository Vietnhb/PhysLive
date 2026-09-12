package com.example.backend.physics.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.entity.ConfirmationState;
import com.example.backend.entity.Specification;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.extraction.PhysicalQuantity;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.validation.ArchetypeInputValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

class SpecificationToSolverInputAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SpecificationToSolverInputAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpecificationToSolverInputAdapter(
                objectMapper,
                new ArchetypeInputValidator(new ArchetypeCatalog()));
    }

    @Test
    void mapsNormalizedQuantitiesToInternalSolverInput() {
        Specification specification = specification(
                "KINEMATICS",
                ConfirmationState.NO_AMBIGUITY,
                List.of(
                        quantity("velocity", "v", "36", "km/h", "10", "m/s"),
                        quantity("time", "t", "5", "s", "5", "s")));

        SolverInput input = adapter.convert(specification, PhysicsArchetype.UNIFORM_MOTION);

        assertThat(input.schemaVersion()).isEqualTo("1.0");
        assertThat(input.archetype()).isEqualTo(PhysicsArchetype.UNIFORM_MOTION);
        assertThat(input.values().get("velocity")).isEqualByComparingTo("10");
        assertThat(input.values().get("duration")).isEqualByComparingTo("5");
        assertThat(input.units()).containsEntry("velocity", "m/s").containsEntry("duration", "s");
    }

    @Test
    void rejectsUnresolvedSpecification() {
        Specification specification = specification(
                "KINEMATICS",
                ConfirmationState.UNRESOLVED,
                List.of(quantity("velocity", "v", "10", "m/s", "10", "m/s")));

        assertThatThrownBy(() -> adapter.convert(specification, PhysicsArchetype.UNIFORM_MOTION))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("unresolved");
    }

    @Test
    void rejectsArchetypeFromAnotherTopic() {
        Specification specification = specification(
                "KINEMATICS",
                ConfirmationState.CONFIRMED,
                List.of(quantity("velocity", "v", "10", "m/s", "10", "m/s")));

        assertThatThrownBy(() -> adapter.convert(specification, PhysicsArchetype.SERIES_CIRCUIT))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void rejectsQuantityWithoutNormalizedValue() {
        Specification specification = specification(
                "KINEMATICS",
                ConfirmationState.CONFIRMED,
                List.of(quantity("velocity", "v", "10", "custom", null, "custom")));

        assertThatThrownBy(() -> adapter.convert(specification, PhysicsArchetype.UNIFORM_MOTION))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("not normalized");
    }

    private Specification specification(
            String topic,
            ConfirmationState confirmationState,
            List<PhysicalQuantity> quantities) {
        Specification specification = new Specification();
        specification.setSchemaVersion("1.0");
        specification.setTopic(topic);
        specification.setConfirmationState(confirmationState);
        specification.setQuantities(objectMapper.valueToTree(quantities));
        return specification;
    }

    private PhysicalQuantity quantity(
            String name,
            String symbol,
            String value,
            String originalUnit,
            String normalizedValue,
            String normalizedUnit) {
        return new PhysicalQuantity(
                name,
                symbol,
                new BigDecimal(value),
                originalUnit,
                normalizedValue == null ? null : new BigDecimal(normalizedValue),
                normalizedUnit,
                new BigDecimal("0.99"),
                value + " " + originalUnit);
    }
}
