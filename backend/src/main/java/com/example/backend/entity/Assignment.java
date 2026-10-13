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
@Table(name = "assignments", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private com.example.backend.entity.school.SchoolClass schoolClass;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CLOSED

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questions", columnDefinition = "jsonb", nullable = false)
    private java.util.Map<String, Object> questions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grading_criteria", columnDefinition = "jsonb")
    private java.util.Map<String, Object> gradingCriteria;

    @Column(name = "max_score", precision = 8, scale = 3, nullable = false)
    private BigDecimal maxScore = BigDecimal.TEN;

    @Column(name = "auto_grade", nullable = false)
    private boolean autoGrade;

    @Column(name = "teacher_id", nullable = false)
    private Integer teacherId;

    @ManyToOne
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private User teacher;

    @Column(name = "specification_id", nullable = false)
    private UUID specificationId;

    @ManyToOne
    @JoinColumn(name = "specification_id", insertable = false, updatable = false)
    private Specification specification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.assignedAt == null) {
            this.assignedAt = now;
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
