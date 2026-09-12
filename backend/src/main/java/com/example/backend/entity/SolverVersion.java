package com.example.backend.entity;

import com.example.backend.physics.model.PhysicsArchetype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "solver_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solver_versions_archetype_type_version",
                columnNames = { "archetype", "solver_type", "version" }),
        indexes = {
                @Index(name = "idx_solver_versions_lookup", columnList = "archetype,solver_type,active"),
                @Index(name = "idx_solver_versions_active", columnList = "active")
        })
@Getter
@Setter
public class SolverVersion extends AuditedEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PhysicsArchetype archetype;

    @Enumerated(EnumType.STRING)
    @Column(name = "solver_type", nullable = false, length = 16)
    private SolverType solverType;

    @Column(nullable = false, length = 64)
    private String version;

    @Column(name = "implementation_class", nullable = false, length = 255)
    private String implementationClass;

    @Column(length = 64)
    private String checksum;

    @Column(nullable = false)
    private boolean active;
}
