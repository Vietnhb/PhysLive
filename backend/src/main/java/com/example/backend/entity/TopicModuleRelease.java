package com.example.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "topic_module_releases",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_topic_module_releases_topic_version",
                columnNames = { "topic_id", "release_version" }),
        indexes = {
                @Index(name = "idx_topic_module_releases_topic_status", columnList = "topic_id,status"),
                @Index(name = "idx_topic_module_releases_status", columnList = "status")
        })
@Getter
@Setter
public class TopicModuleRelease extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "release_version", nullable = false, length = 32)
    private String releaseVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schema_version_id", nullable = false)
    private SchemaVersion schemaVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "numerical_solver_version_id", nullable = false)
    private SolverVersion numericalSolverVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reference_solver_version_id", nullable = false)
    private SolverVersion referenceSolverVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModuleReleaseStatus status;

    private Instant approvedAt;

    private Instant retiredAt;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "retired_by")
    private Integer retiredBy;
}
