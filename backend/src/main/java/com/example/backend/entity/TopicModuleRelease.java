package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

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
@Table(name = "topic_module_releases", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopicModuleRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "schema_id", nullable = false)
    private String schemaId;

    @Column(name = "schema_version", nullable = false)
    private String schemaVersion;

    @Column(name = "lifecycle_status", nullable = false)
    private String lifecycleStatus = "DRAFT"; // DRAFT, APPROVED, RETIRED

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
