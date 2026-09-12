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
public class ReviewerDecisionResponse {
    private UUID id;
    private UUID ambiguityCaseId;
    private Integer actorId;
    private String actorRole;
    private String answer;
    private String comment;
    private String decisionState;
    private OffsetDateTime decidedAt;
    private OffsetDateTime createdAt;
}
