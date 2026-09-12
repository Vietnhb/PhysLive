package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ReviewerDecision;

@Repository
public interface ReviewerDecisionRepository extends JpaRepository<ReviewerDecision, UUID> {
    List<ReviewerDecision> findByAmbiguityCaseIdOrderByDecidedAtDesc(UUID ambiguityCaseId);
}
