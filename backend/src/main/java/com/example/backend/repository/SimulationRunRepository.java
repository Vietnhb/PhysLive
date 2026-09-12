package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.SimulationRun;
import com.example.backend.entity.User;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {
    List<SimulationRun> findBySimulationIdOrderByCreatedAtDesc(UUID simulationId);

    Optional<SimulationRun> findByIdAndSimulationValidationRunSpecificationSubmissionOwner(
            UUID id, User owner);
}
