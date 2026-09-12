package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ExtractionOutcome;
import com.example.backend.entity.ExtractionPath;
import com.example.backend.entity.ExtractionRun;
import com.example.backend.entity.ExtractionRunStatus;

@Repository
public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {
    List<ExtractionRun> findTop20ByOrderByCreatedAtDesc();
    List<ExtractionRun> findByStatusOrderByCreatedAtDesc(ExtractionRunStatus status);
    long countByStatus(ExtractionRunStatus status);
    long countByOutcome(ExtractionOutcome outcome);
    long countByExtractionPath(ExtractionPath extractionPath);
    long countByProviderName(String providerName);
}
