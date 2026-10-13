package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignment_submissions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @ManyToOne
    @JoinColumn(name = "assignment_id", insertable = false, updatable = false)
    private Assignment assignment;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @ManyToOne
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "predictions", columnDefinition = "jsonb", nullable = false)
    private java.util.Map<String, Object> predictions;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "score", precision = 8, scale = 3)
    private BigDecimal score;

    @Column(name = "max_score", precision = 8, scale = 3)
    private BigDecimal maxScore;

    @Column(name = "feedback", columnDefinition = "text")
    private String feedback;

    @Column(name = "grading_status", nullable = false, length = 24)
    private String gradingStatus = "PENDING";

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    @Column(name = "graded_by")
    private Integer gradedBy;

    @Column(name = "retry_allowed", nullable = false)
    private boolean retryAllowed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.submittedAt == null) {
            this.submittedAt = now;
        }
        if (this.gradingStatus == null) {
            this.gradingStatus = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
