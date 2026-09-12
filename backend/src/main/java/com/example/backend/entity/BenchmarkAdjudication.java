package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "benchmark_adjudications", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkAdjudication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "benchmark_problem_id", nullable = false)
    private UUID benchmarkProblemId;

    @Column(name = "reviewer_reference", nullable = false)
    private String reviewerReference;

    @Column(name = "disagreement_state", nullable = false)
    private String disagreementState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolved_specification", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> resolvedSpecification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
