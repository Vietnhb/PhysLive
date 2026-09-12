package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.ValidationRun;
import com.example.backend.entity.User;

import java.util.Optional;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, UUID> {
    List<ValidationRun> findBySpecificationIdOrderByCreatedAtDesc(UUID specificationId);

    Optional<ValidationRun> findByIdAndSpecificationSubmissionOwner(UUID id, User owner);
}
