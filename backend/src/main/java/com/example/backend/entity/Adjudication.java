package com.example.backend.entity;

import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "benchmark_adjudications")
@Getter
@Setter
@NoArgsConstructor
public class Adjudication extends AuditedEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "benchmark_problem_id", nullable = false)
    private BenchmarkProblem benchmarkProblem;

    @Column(nullable = false, length = 80)
    private String reviewerReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode resolvedSpecification;

    @Column(nullable = false, length = 24)
    private String disagreementState;

    public UUID getBenchmarkProblemId() {
        return benchmarkProblem != null ? benchmarkProblem.getId() : null;
    }

    public void setResolvedSpecification(Map<String, Object> map) {
        if (map == null) this.resolvedSpecification = null;
        else this.resolvedSpecification = MAPPER.valueToTree(map);
    }

    public void setResolvedSpecification(JsonNode node) {
        this.resolvedSpecification = node;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getResolvedSpecificationAsMap() {
        if (resolvedSpecification == null) return null;
        return MAPPER.convertValue(resolvedSpecification, Map.class);
    }
}
