package com.example.backend.system.problem.repository;

import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.problem.model.entity.Specification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecificationRepository extends JpaRepository<Specification, UUID> {
    Optional<Specification> findByIdAndSubmissionOwner(UUID id, User owner);
}
