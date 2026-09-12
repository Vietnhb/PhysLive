package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lessons", uniqueConstraints = @UniqueConstraint(name = "uk_lessons_level_slug", columnNames = { "level_id", "slug" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "level_id", nullable = false)
    private GradeLevel level;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;
}
