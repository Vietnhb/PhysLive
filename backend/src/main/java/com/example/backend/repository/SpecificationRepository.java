package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Specification;
import com.example.backend.entity.User;

@Repository
public interface SpecificationRepository extends JpaRepository<Specification, UUID> {
    Optional<Specification> findByIdAndSubmissionOwner(UUID id, User owner);
}
