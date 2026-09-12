package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.BenchmarkAdjudication;

@Repository
public interface BenchmarkAdjudicationRepository extends JpaRepository<BenchmarkAdjudication, UUID> {
    List<BenchmarkAdjudication> findByBenchmarkProblemId(UUID benchmarkProblemId);
}
