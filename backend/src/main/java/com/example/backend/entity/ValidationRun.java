package com.example.backend.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.backend.physics.model.PhysicsArchetype;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "validation_runs",
        indexes = {
                @Index(name = "idx_validation_runs_specification", columnList = "specification_id,created_at"),
                @Index(name = "idx_validation_runs_status", columnList = "status")
        })
@Getter
@Setter
public class ValidationRun extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specification_id", nullable = false)
    private Specification specification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_release_id")
    private TopicModuleRelease moduleRelease;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schema_version_id", nullable = false)
    private SchemaVersion schemaVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "numerical_solver_version_id", nullable = false)
    private SolverVersion numericalSolverVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reference_solver_version_id", nullable = false)
    private SolverVersion referenceSolverVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PhysicsArchetype archetype;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ValidationRunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode inputPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "numerical_output", columnDefinition = "jsonb")
    private JsonNode numericalOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reference_output", columnDefinition = "jsonb")
    private JsonNode referenceOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "absolute_errors", columnDefinition = "jsonb")
    private JsonNode absoluteErrors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "relative_errors", columnDefinition = "jsonb")
    private JsonNode relativeErrors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tolerances", columnDefinition = "jsonb")
    private JsonNode tolerances;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    private Instant startedAt;

    private Instant completedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
}
