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
public class BenchmarkAdjudicationResponse {
    private UUID id;
    private UUID benchmarkProblemId;
    private String reviewerReference;
    private String disagreementState;
    private Map<String, Object> resolvedSpecification;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
