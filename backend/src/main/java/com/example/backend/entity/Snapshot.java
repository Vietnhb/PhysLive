package com.example.backend.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_snapshots_run_sequence",
                columnNames = { "simulation_run_id", "sequence_number" }),
        indexes = @Index(
                name = "idx_snapshots_run_time",
                columnList = "simulation_run_id,simulation_time"))
@Getter
@Setter
public class Snapshot extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_run_id", nullable = false)
    private SimulationRun simulationRun;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "simulation_time", nullable = false, precision = 20, scale = 9)
    private BigDecimal simulationTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode statePayload;
}
