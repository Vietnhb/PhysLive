package com.example.backend.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
public class SimulationRun extends AuditedEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solver_version_id")
    private SolverVersion solverVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SimulationRunStatus status = SimulationRunStatus.COMPLETED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_parameters", columnDefinition = "jsonb")
    private JsonNode inputParameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_payload", columnDefinition = "jsonb")
    private JsonNode outputPayload;

    @Column(name = "frame_count", nullable = false)
    private int frameCount = 0;

    private Instant startedAt;

    private Instant completedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "duration_seconds", nullable = false)
    private Double durationSeconds = 0.0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private JsonNode result;

    @Column(name = "run_type", nullable = false, length = 32)
    private String runType = "INTERACTIVE";

    @Column(name = "validation_passed", nullable = false)
    private boolean validationPassed = true;

    public UUID getSimulationId() {
        return simulation != null ? simulation.getId() : null;
    }

    public void setSimulationId(UUID id) {
        // simulation is set directly via setSimulation
    }

    public void setResult(Map<String, Object> map) {
        if (map == null) this.result = null;
        else this.result = MAPPER.valueToTree(map);
    }

    public void setResult(JsonNode resultNode) {
        this.result = resultNode;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResult() {
        if (result == null) return null;
        return MAPPER.convertValue(result, Map.class);
    }
}
