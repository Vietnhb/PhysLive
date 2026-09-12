package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ReviewerDecision;

@Repository
public interface ReviewerDecisionRepository extends JpaRepository<ReviewerDecision, UUID> {
    @Query("select d from ReviewerDecision d where d.ambiguityCase.id = :ambiguityCaseId order by d.decidedAt desc")
    List<ReviewerDecision> findByAmbiguityCaseIdOrderByDecidedAtDesc(@Param("ambiguityCaseId") UUID ambiguityCaseId);
}
