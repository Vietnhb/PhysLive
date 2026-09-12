package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.SimulationRun;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {
    List<SimulationRun> findBySimulationIdOrderByCreatedAtDesc(UUID simulationId);
}
