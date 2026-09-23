package com.example.backend.entity;

import java.time.OffsetDateTime;
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
@Table(name = "class_sessions", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "duration_seconds", nullable = false)
    private Double durationSeconds = 0.0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initial_specification", columnDefinition = "jsonb", nullable = false)
    private java.util.Map<String, Object> initialSpecification;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameter_timeline", columnDefinition = "jsonb", nullable = false)
    private java.util.List<java.util.Map<String, Object>> parameterTimeline;

    @Column(name = "storage_url")
    private String storageUrl;

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
        if (this.durationSeconds == null) {
            this.durationSeconds = 0.0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
