package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Simulation;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, UUID> {

    List<Simulation> findByOwnerIdOrderByCreatedAtDesc(Integer ownerId);

    Optional<Simulation> findBySpecificationId(UUID specificationId);
}
