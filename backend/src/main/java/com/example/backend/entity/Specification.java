package com.example.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
@Table(name = "specifications", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Specification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "schema_version", nullable = false)
    private String schemaVersion = "v1.0";

    @Column(name = "schema_id")
    private String schemaId;

    @Column(name = "topic")
    private String topic;

    @Column(name = "confidence", nullable = false)
    private BigDecimal confidence = BigDecimal.ONE;

    @Column(name = "confirmation_state", nullable = false)
    private String confirmationState = "NO_AMBIGUITY"; // NO_AMBIGUITY, UNRESOLVED, CONFIRMED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objects", columnDefinition = "jsonb", nullable = false)
    private Object objects;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quantities", columnDefinition = "jsonb", nullable = false)
    private Object quantities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "relations", columnDefinition = "jsonb", nullable = false)
    private Object relations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ambiguity", columnDefinition = "jsonb", nullable = false)
    private Object ambiguity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_result", columnDefinition = "jsonb")
    private Object validationResult;

    @Column(name = "validation_status")
    private String validationStatus;

    @Column(name = "extraction_run_id", nullable = false)
    private UUID extractionRunId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.confidence == null) {
            this.confidence = BigDecimal.ONE;
        }
        if (this.confirmationState == null) {
            this.confirmationState = "NO_AMBIGUITY";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
