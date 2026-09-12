package com.example.backend.entity;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.backend.physics.model.PhysicsArchetype;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "solver_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solver_versions_archetype_type_version",
                columnNames = { "archetype", "solver_type", "version" }),
        indexes = {
                @Index(name = "idx_solver_versions_lookup", columnList = "archetype,solver_type,active"),
                @Index(name = "idx_solver_versions_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
public class SolverVersion extends AuditedEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PhysicsArchetype archetype = PhysicsArchetype.UNIFORM_MOTION;


    @Enumerated(EnumType.STRING)
    @Column(name = "solver_type", nullable = false, length = 16)
    private SolverType solverType = SolverType.NUMERICAL;

    @Column(nullable = false, length = 64)
    private String version;

    @Column(name = "implementation_class", length = 255)
    private String implementationClass;

    @Column(length = 64)
    private String checksum;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 64)
    private String solverId;

    @Column(length = 64)
    private String schemaId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_definition", columnDefinition = "jsonb")
    private JsonNode outputDefinition;

    @Column(length = 32)
    private String lifecycleStatus = "APPROVED";

    public void setOutputDefinition(Map<String, Object> map) {
        if (map == null) this.outputDefinition = null;
        else this.outputDefinition = MAPPER.valueToTree(map);
    }

    public void setOutputDefinition(JsonNode node) {
        this.outputDefinition = node;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getOutputDefinitionAsMap() {
        if (outputDefinition == null) return null;
        return MAPPER.convertValue(outputDefinition, Map.class);
    }
}
