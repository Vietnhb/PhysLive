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
public class ExtractionRunResponse {
    private UUID id;
    private UUID submissionId;
    private String providerName;
    private String extractionPath;
    private String modelVersion;
    private String status;
    private String outcome;
    private String errorMessage;
    private OffsetDateTime createdAt;
}
