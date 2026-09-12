package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(Integer teacherId);

    List<Assignment> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT a FROM Assignment a JOIN AssignmentStudent s ON a.id = s.id.assignmentId WHERE s.id.studentId = :studentId ORDER BY a.createdAt DESC")
    List<Assignment> findAssignmentsForStudent(@Param("studentId") Integer studentId);
}
