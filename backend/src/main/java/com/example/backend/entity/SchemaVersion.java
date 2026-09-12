package com.example.backend.entity;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "schema_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_schema_versions_version", columnNames = "version"),
        indexes = @Index(name = "idx_schema_versions_active", columnList = "active"))
@Getter
@Setter
@NoArgsConstructor
public class SchemaVersion extends AuditedEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Column(length = 64)
    private String schemaId;

    @Column(length = 64)
    private String topic;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode definition;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 32)
    private String lifecycleStatus = "APPROVED";

    public void setDefinition(Map<String, Object> map) {
        if (map == null) {
            this.definition = null;
        } else {
            this.definition = MAPPER.valueToTree(map);
        }
    }

    public void setDefinition(JsonNode definition) {
        this.definition = definition;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDefinitionAsMap() {
        if (definition == null) return null;
        return MAPPER.convertValue(definition, Map.class);
    }
}
