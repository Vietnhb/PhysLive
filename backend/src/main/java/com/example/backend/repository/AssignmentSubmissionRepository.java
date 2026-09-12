package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.AssignmentSubmission;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDesc(UUID assignmentId);

    List<AssignmentSubmission> findByStudentIdOrderBySubmittedAtDesc(Integer studentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, Integer studentId);

    boolean existsByAssignmentIdAndStudentId(UUID assignmentId, Integer studentId);
}
