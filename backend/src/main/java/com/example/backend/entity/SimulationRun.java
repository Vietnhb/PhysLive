package com.example.backend.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
        name = "simulation_runs",
        indexes = {
                @Index(name = "idx_simulation_runs_simulation", columnList = "simulation_id,created_at"),
                @Index(name = "idx_simulation_runs_status", columnList = "status")
        })
@Getter
@Setter
public class SimulationRun extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solver_version_id", nullable = false)
    private SolverVersion solverVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SimulationRunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_parameters", nullable = false, columnDefinition = "jsonb")
    private JsonNode inputParameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_payload", columnDefinition = "jsonb")
    private JsonNode outputPayload;

    @Column(name = "frame_count", nullable = false)
    private int frameCount;

    private Instant startedAt;

    private Instant completedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
