package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Simulation;
import com.example.backend.entity.User;

import jakarta.persistence.LockModeType;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, UUID> {
    Optional<Simulation> findByValidationRunId(UUID validationRunId);

    Optional<Simulation> findByIdAndValidationRunSpecificationSubmissionOwner(UUID id, User owner);

    @Query("select s from Simulation s where s.owner.id = :ownerId order by s.createdAt desc")
    List<Simulation> findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Integer ownerId);

    @Query("""
            select simulation from Simulation simulation
            join simulation.validationRun validation
            join validation.specification specification
            join specification.submission submission
            where submission.owner = :owner
            order by simulation.createdAt desc
            """)
    List<Simulation> findOwnedOrderByCreatedAtDesc(@Param("owner") User owner);

    @Query("select s from Simulation s where s.specification.id = :specificationId")
    Optional<Simulation> findBySpecificationId(@Param("specificationId") UUID specificationId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select simulation
            from Simulation simulation
            join simulation.validationRun validation
            join validation.specification specification
            join specification.submission submission
            where simulation.id = :id and submission.owner = :owner
            """)
    Optional<Simulation> findOwnedForRunCreation(
            @Param("id") UUID id,
            @Param("owner") User owner);
}
