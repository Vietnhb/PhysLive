package com.example.backend.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "schema_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_schema_versions_version", columnNames = "version"),
        indexes = @Index(name = "idx_schema_versions_active", columnList = "active"))
@Getter
@Setter
public class SchemaVersion extends AuditedEntity {

    @Column(nullable = false, length = 32)
    private String version;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode definition;

    @Column(nullable = false)
    private boolean active;
}
