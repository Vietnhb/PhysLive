package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.BenchmarkProblem;

@Repository
public interface BenchmarkProblemRepository extends JpaRepository<BenchmarkProblem, UUID> {
    List<BenchmarkProblem> findByTopic(String topic);
    List<BenchmarkProblem> findByActiveTrue();
}
