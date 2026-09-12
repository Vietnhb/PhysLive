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
public class SolverVersionResponse {
    private UUID id;
    private String solverId;
    private String schemaId;
    private String version;
    private Map<String, Object> outputDefinition;
    private String lifecycleStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
