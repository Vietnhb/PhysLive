package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.AmbiguityCase;

@Repository
public interface AmbiguityCaseRepository extends JpaRepository<AmbiguityCase, UUID> {
    List<AmbiguityCase> findByStatus(String status);
    List<AmbiguityCase> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
