package com.example.backend.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "simulations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_simulations_validation_run",
                columnNames = "validation_run_id"),
        indexes = @Index(name = "idx_simulations_status", columnList = "status"))
@Getter
@Setter
public class Simulation extends AuditedEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validation_run_id", nullable = false)
    private ValidationRun validationRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SimulationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initial_parameters", nullable = false, columnDefinition = "jsonb")
    private JsonNode initialParameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scene_definition", columnDefinition = "jsonb")
    private JsonNode sceneDefinition;
}
