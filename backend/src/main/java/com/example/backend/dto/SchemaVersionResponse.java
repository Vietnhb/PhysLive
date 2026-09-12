package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchemaVersionResponse {
    private UUID id;
    private String schemaId;
    private String topic;
    private String version;
    private String name;
    private Map<String, Object> definition;
    private boolean enabled;
    private String lifecycleStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
