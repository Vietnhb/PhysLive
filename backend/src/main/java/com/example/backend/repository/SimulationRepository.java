package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.Simulation;
import com.example.backend.entity.User;

import jakarta.persistence.LockModeType;

public interface SimulationRepository extends JpaRepository<Simulation, UUID> {
    Optional<Simulation> findByValidationRunId(UUID validationRunId);

    Optional<Simulation> findByIdAndValidationRunSpecificationSubmissionOwner(UUID id, User owner);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select simulation
            from Simulation simulation
            join simulation.validationRun validation
            join validation.specification specification
            join specification.submission submission
            where simulation.id = :id and submission.owner = :owner
            """)
    Optional<Simulation> findOwnedForRunCreation(
            @Param("id") UUID id,
            @Param("owner") User owner);
}
