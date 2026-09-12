package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.AmbiguityCase;
import com.example.backend.entity.AmbiguityStatus;
import com.example.backend.entity.Specification;

@Repository
public interface AmbiguityCaseRepository extends JpaRepository<AmbiguityCase, UUID> {
    List<AmbiguityCase> findByStatus(AmbiguityStatus status);
    List<AmbiguityCase> findAllByOrderByCreatedAtDesc();
    long countByStatus(AmbiguityStatus status);
    Optional<AmbiguityCase> findByIdAndSpecification(UUID id, Specification specification);
}
