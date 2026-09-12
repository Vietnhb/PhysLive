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
public class BenchmarkProblemResponse {
    private UUID id;
    private String topic;
    private String gradeScope;
    private String sourceCategory;
    private String problemText;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
