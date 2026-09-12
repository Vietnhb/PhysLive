package com.example.backend.physics.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.backend.entity.ConfirmationState;
import com.example.backend.entity.Specification;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.extraction.PhysicalQuantity;
import com.example.backend.extraction.PhysicalRelation;
import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.validation.ArchetypeInputValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

class SpecificationArchetypeDetectionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SpecificationToSolverInputAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpecificationToSolverInputAdapter(
                objectMapper,
                new ArchetypeInputValidator(new ArchetypeCatalog()));
    }

    @ParameterizedTest
    @MethodSource("specificationsForAllArchetypes")
    void detectsAndMapsEverySupportedArchetype(
            Specification specification,
            PhysicsArchetype expectedArchetype) {
        SolverInput input = adapter.convert(specification);

        assertThat(input.archetype()).isEqualTo(expectedArchetype);
        if (expectedArchetype == PhysicsArchetype.SERIES_CIRCUIT
                || expectedArchetype == PhysicsArchetype.PARALLEL_CIRCUIT) {
            assertThat(input.seriesValues().get("resistances")).hasSize(2);
        }
    }

    @ParameterizedTest
    @MethodSource("ambiguousSpecifications")
    void refusesToGuessWhenEvidenceIsInsufficient(Specification specification, String expectedMessage) {
        assertThatThrownBy(() -> adapter.convert(specification))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining(expectedMessage);
    }

    private StreamBuilder specifications() {
        return new StreamBuilder();
    }

    private static java.util.stream.Stream<Arguments> specificationsForAllArchetypes() {
        SpecificationArchetypeDetectionTest factory = new SpecificationArchetypeDetectionTest();
        return factory.specifications().valid();
    }

    private static java.util.stream.Stream<Arguments> ambiguousSpecifications() {
        SpecificationArchetypeDetectionTest factory = new SpecificationArchetypeDetectionTest();
        return factory.specifications().ambiguous();
    }

    private final class StreamBuilder {
        java.util.stream.Stream<Arguments> valid() {
            return java.util.stream.Stream.of(
                    Arguments.of(specification("KINEMATICS", List.of(
                            quantity("velocity", "10", "m/s"), quantity("time", "5", "s")), List.of()),
                            PhysicsArchetype.UNIFORM_MOTION),
                    Arguments.of(specification("KINEMATICS", List.of(
                            quantity("acceleration", "2", "m/s2"), quantity("time", "5", "s")), List.of()),
                            PhysicsArchetype.CONSTANT_ACCELERATION),
                    Arguments.of(specification("DYNAMICS", List.of(
                            quantity("mass", "2", "kg"), quantity("force", "10", "N"),
                            quantity("time", "3", "s")), List.of()), PhysicsArchetype.HORIZONTAL_FORCE),
                    Arguments.of(specification("DYNAMICS", List.of(
                            quantity("mass", "2", "kg"), quantity("angle", "0.5", "rad"),
                            quantity("time", "3", "s")), List.of()), PhysicsArchetype.INCLINED_PLANE),
                    Arguments.of(circuit("SERIES"), PhysicsArchetype.SERIES_CIRCUIT),
                    Arguments.of(circuit("PARALLEL"), PhysicsArchetype.PARALLEL_CIRCUIT));
        }

        java.util.stream.Stream<Arguments> ambiguous() {
            return java.util.stream.Stream.of(
                    Arguments.of(specification("KINEMATICS", List.of(quantity("distance", "10", "m")), List.of()),
                            "Cannot identify"),
                    Arguments.of(circuit(null), "explicit SERIES or PARALLEL"));
        }
    }

    private Specification circuit(String relationType) {
        List<PhysicalRelation> relations = relationType == null
                ? List.of()
                : List.of(new PhysicalRelation(relationType, "R1", "R2", null, null, relationType));
        return specification("CIRCUITS", List.of(
                quantity("voltage", "12", "V"),
                quantity("resistance", "10", "ohm"),
                quantity("resistance", "20", "ohm")), relations);
    }

    private Specification specification(
            String topic,
            List<PhysicalQuantity> quantities,
            List<PhysicalRelation> relations) {
        Specification specification = new Specification();
        specification.setSchemaVersion("1.0");
        specification.setTopic(topic);
        specification.setConfirmationState(ConfirmationState.NO_AMBIGUITY);
        specification.setQuantities(objectMapper.valueToTree(quantities));
        specification.setRelations(objectMapper.valueToTree(relations));
        return specification;
    }

    private PhysicalQuantity quantity(String name, String value, String unit) {
        BigDecimal number = new BigDecimal(value);
        return new PhysicalQuantity(name, null, number, unit, number, unit, BigDecimal.ONE, value + " " + unit);
    }
}
