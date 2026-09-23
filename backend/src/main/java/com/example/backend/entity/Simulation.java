package com.example.backend.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
public class Simulation extends AuditedEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_run_id")
    private ValidationRun validationRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SimulationStatus status = SimulationStatus.READY;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initial_parameters", columnDefinition = "jsonb")
    private JsonNode initialParameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scene_definition", columnDefinition = "jsonb")
    private JsonNode sceneDefinition;

    @Column(length = 64)
    private String schemaId;

    @Column(length = 64)
    private String solverVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "latest_result", columnDefinition = "jsonb")
    private JsonNode latestResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specification_id")
    private Specification specification;

    public Integer getOwnerId() {
        return owner != null ? owner.getId() : null;
    }

    public UUID getSpecificationId() {
        return specification != null ? specification.getId() : null;
    }

    public void setLatestResult(Map<String, Object> map) {
        if (map == null) this.latestResult = null;
        else this.latestResult = MAPPER.valueToTree(map);
    }

    public void setLatestResult(JsonNode node) {
        this.latestResult = node;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLatestResult() {
        if (latestResult == null) return null;
        return MAPPER.convertValue(latestResult, Map.class);
    }
}
