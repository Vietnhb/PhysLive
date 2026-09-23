package com.example.backend.physics.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArchetypeCatalogTest {

    private final ArchetypeCatalog catalog = new ArchetypeCatalog();

    @Test
    void definesAtLeastTwoArchetypesForEachSupportedTopic() {
        assertThat(catalog.all()).hasSize(PhysicsArchetype.values().length);
        assertThat(catalog.all().keySet())
                .filteredOn(archetype -> archetype.belongsTo("KINEMATICS"))
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(catalog.all().keySet())
                .filteredOn(archetype -> archetype.belongsTo("DYNAMICS"))
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(catalog.all().keySet())
                .filteredOn(archetype -> archetype.belongsTo("CIRCUITS"))
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void everyArchetypeDefinesInputsOutputsAndSiUnits() {
        catalog.all().values().forEach(definition -> {
            assertThat(definition.inputs()).isNotEmpty();
            assertThat(definition.outputs()).isNotEmpty();
            assertThat(definition.inputs()).allSatisfy(input -> {
                assertThat(input.key()).isNotBlank();
                assertThat(input.unit()).isNotBlank();
            });
            assertThat(definition.outputs()).allSatisfy(output -> {
                assertThat(output.key()).isNotBlank();
                assertThat(output.unit()).isNotBlank();
            });
        });
    }

    @Test
    void circuitDefinitionsRequireAResistanceSeries() {
        assertThat(catalog.require(PhysicsArchetype.SERIES_CIRCUIT).input("resistances"))
                .hasValueSatisfying(input -> {
                    assertThat(input.required()).isTrue();
                    assertThat(input.cardinality()).isEqualTo(QuantityCardinality.SERIES);
                    assertThat(input.unit()).isEqualTo("ohm");
                });
        assertThat(catalog.require(PhysicsArchetype.PARALLEL_CIRCUIT).input("resistances"))
                .isPresent();
    }
}
