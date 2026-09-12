package com.example.backend.entity;

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
@Table(name = "extraction_runs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "extraction_path", nullable = false)
    private String extractionPath; // OPENROUTER, RULE_BASED

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "status", nullable = false)
    private String status; // RUNNING, SUCCEEDED, FAILED

    @Column(name = "outcome")
    private String outcome; // API_SUCCESS, RULE_BASED_FALLBACK, FAILED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Object rawResponse;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

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
