package com.example.backend.system.simulation.repository;

import com.example.backend.system.simulation.model.entity.SimulationRun;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {
    Optional<SimulationRun> findFirstBySimulationIdOrderByCreatedAtDesc(UUID simulationId);
    Optional<SimulationRun> findFirstBySimulationIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
            UUID simulationId, Instant timestamp);
    long countByValidationPassedFalse();
}
