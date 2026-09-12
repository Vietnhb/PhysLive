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
public class AmbiguityCaseResponse {
    private UUID id;
    private UUID specificationId;
    private String code;
    private String fieldPath;
    private String question;
    private Object options;
    private String resolution;
    private OffsetDateTime resolvedAt;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
