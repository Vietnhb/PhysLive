package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ExtractionRun;

@Repository
public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {
    List<ExtractionRun> findTop20ByOrderByCreatedAtDesc();
    List<ExtractionRun> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
    long countByOutcome(String outcome);
    long countByExtractionPath(String extractionPath);
    long countByProviderName(String providerName);
}
