package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Simulation;

public interface SimulationRepository extends JpaRepository<Simulation, UUID> {
    Optional<Simulation> findByValidationRunId(UUID validationRunId);
}
