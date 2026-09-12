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
@Table(name = "ambiguity_cases", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AmbiguityCase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "specification_id", nullable = false)
    private UUID specificationId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "field_path", nullable = false)
    private String fieldPath;

    @Column(name = "question", nullable = false, columnDefinition = "text")
    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private Object options;

    @Column(name = "resolution", columnDefinition = "text")
    private String resolution;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, RESOLVED, REJECTED

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
