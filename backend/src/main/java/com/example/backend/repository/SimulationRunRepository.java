package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.SimulationRun;
import com.example.backend.entity.User;

@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {
    @Query("select r from SimulationRun r where r.simulation.id = :simulationId order by r.createdAt desc")
    List<SimulationRun> findBySimulationIdOrderByCreatedAtDesc(@Param("simulationId") UUID simulationId);

    Optional<SimulationRun> findByIdAndSimulationValidationRunSpecificationSubmissionOwner(
            UUID id, User owner);
}
