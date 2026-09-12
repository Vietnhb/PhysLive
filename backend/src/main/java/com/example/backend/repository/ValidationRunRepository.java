package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.entity.ValidationRun;
import com.example.backend.entity.User;

import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, UUID> {
    List<ValidationRun> findBySpecificationIdOrderByCreatedAtDesc(UUID specificationId);

    Optional<ValidationRun> findByIdAndSpecificationSubmissionOwner(UUID id, User owner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select validation
            from ValidationRun validation
            join validation.specification specification
            join specification.submission submission
            where validation.id = :id and submission.owner = :owner
            """)
    Optional<ValidationRun> findOwnedForSimulationGate(
            @Param("id") UUID id,
            @Param("owner") User owner);
}
