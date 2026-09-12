package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Adjudication;

@Repository
public interface AdjudicationRepository extends JpaRepository<Adjudication, UUID> {
    @Query("select a from Adjudication a where a.benchmarkProblem.id = :benchmarkProblemId")
    List<Adjudication> findByBenchmarkProblemId(@Param("benchmarkProblemId") UUID benchmarkProblemId);
}
