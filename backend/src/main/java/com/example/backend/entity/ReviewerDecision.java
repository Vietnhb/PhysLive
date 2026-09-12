package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviewer_decisions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ambiguity_case_id", nullable = false)
    private UUID ambiguityCaseId;

    @Column(name = "actor_id", nullable = false)
    private Integer actorId;

    @Column(name = "actor_role", nullable = false)
    private String actorRole;

    @Column(name = "answer", nullable = false, columnDefinition = "text")
    private String answer;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "decision_state", nullable = false)
    private String decisionState;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (decidedAt == null) decidedAt = OffsetDateTime.now();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
