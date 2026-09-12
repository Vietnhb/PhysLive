package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.SolverType;
import com.example.backend.entity.SolverVersion;
import com.example.backend.physics.model.PhysicsArchetype;

public interface SolverVersionRepository extends JpaRepository<SolverVersion, UUID> {
    Optional<SolverVersion> findByArchetypeAndSolverTypeAndVersion(
            PhysicsArchetype archetype, SolverType solverType, String version);

    List<SolverVersion> findByArchetypeAndActiveTrue(PhysicsArchetype archetype);
}
