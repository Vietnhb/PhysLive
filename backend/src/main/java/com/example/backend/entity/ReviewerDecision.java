package com.example.backend.entity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviewer_decisions")
@Getter
@Setter
@NoArgsConstructor
public class ReviewerDecision extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambiguity_case_id", nullable = false)
    private AmbiguityCase ambiguityCase;

    @Column(nullable = false)
    private Integer actorId;

    @Column(nullable = false, length = 24)
    private String actorRole;

    @Column(nullable = false, length = 24)
    private String decisionState;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    private Instant decidedAt;

    public UUID getAmbiguityCaseId() {
        return ambiguityCase != null ? ambiguityCase.getId() : null;
    }

    public void setDecidedAt(OffsetDateTime dateTime) {
        this.decidedAt = dateTime != null ? dateTime.toInstant() : null;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
