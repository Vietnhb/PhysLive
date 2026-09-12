package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopicModuleReleaseResponse {
    private UUID id;
    private String topic;
    private String moduleName;
    private String schemaId;
    private String schemaVersion;
    private String lifecycleStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
