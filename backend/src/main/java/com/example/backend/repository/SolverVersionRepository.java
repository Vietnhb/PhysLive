package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.SolverVersion;

@Repository
public interface SolverVersionRepository extends JpaRepository<SolverVersion, UUID> {
    List<SolverVersion> findBySchemaId(String schemaId);
    List<SolverVersion> findByLifecycleStatus(String lifecycleStatus);
}
