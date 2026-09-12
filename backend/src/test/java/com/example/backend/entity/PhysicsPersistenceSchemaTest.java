package com.example.backend.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

class PhysicsPersistenceSchemaTest {

    @Test
    void physicsPersistenceTypesUseAuditedUuidEntitiesAndExpectedTables() {
        Map<Class<?>, String> expectedTables = Map.of(
                SchemaVersion.class, "schema_versions",
                SolverVersion.class, "solver_versions",
                TopicModuleRelease.class, "topic_module_releases",
                ValidationRun.class, "validation_runs",
                Simulation.class, "simulations",
                SimulationRun.class, "simulation_runs",
                Snapshot.class, "snapshots");

        expectedTables.forEach((type, tableName) -> {
            assertThat(type).hasAnnotation(Entity.class);
            assertThat(type.getSuperclass()).isEqualTo(AuditedEntity.class);
            assertThat(type.getAnnotation(Table.class).name()).isEqualTo(tableName);
        });
    }

    @Test
    void persistenceConstraintsProtectVersionAndReplayIdentity() {
        assertThat(uniqueConstraintNames(SchemaVersion.class))
                .contains("uk_schema_versions_version");
        assertThat(uniqueConstraintNames(SolverVersion.class))
                .contains("uk_solver_versions_archetype_type_version");
        assertThat(uniqueConstraintNames(TopicModuleRelease.class))
                .contains("uk_topic_module_releases_topic_version");
        assertThat(uniqueConstraintNames(Simulation.class))
                .contains("uk_simulations_validation_run");
        assertThat(uniqueConstraintNames(Snapshot.class))
                .contains("uk_snapshots_run_sequence");
    }

    private String[] uniqueConstraintNames(Class<?> entityType) {
        return java.util.Arrays.stream(entityType.getAnnotation(Table.class).uniqueConstraints())
                .map(jakarta.persistence.UniqueConstraint::name)
                .toArray(String[]::new);
    }
}
