package com.example.backend.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "specifications")
@Getter
@Setter
@NoArgsConstructor
public class Specification extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private ProblemSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_run_id", nullable = false)
    private ExtractionRun extractionRun;

    @Column(nullable = false, length = 16)
    private String schemaVersion = "v1.0";

    @Column(length = 64)
    private String schemaId;

    @Column(length = 32)
    private String topic;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence = BigDecimal.ONE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode objects;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode quantities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode relations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "end_condition", columnDefinition = "jsonb")
    private JsonNode endCondition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode ambiguity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ConfirmationState confirmationState = ConfirmationState.NO_AMBIGUITY;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_result", columnDefinition = "jsonb")
    private JsonNode validationResult;

    @Column(name = "validation_status", length = 32)
    private String validationStatus;

    @OneToMany(mappedBy = "specification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<AmbiguityCase> ambiguityCases = new ArrayList<>();

    public void addAmbiguityCase(AmbiguityCase ambiguityCase) {
        ambiguityCases.add(ambiguityCase);
        ambiguityCase.setSpecification(this);
    }

    public UUID getSubmissionId() {
        return submission != null ? submission.getId() : null;
    }

    public UUID getExtractionRunId() {
        return extractionRun != null ? extractionRun.getId() : null;
    }

    public void setConfirmationState(ConfirmationState state) {
        this.confirmationState = state;
    }

    public void setConfirmationState(String stateStr) {
        if (stateStr != null) {
            this.confirmationState = ConfirmationState.valueOf(stateStr.toUpperCase());
        }
    }
}
