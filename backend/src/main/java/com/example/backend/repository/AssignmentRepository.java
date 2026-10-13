package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(Integer teacherId);

    List<Assignment> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT a FROM Assignment a JOIN AssignmentStudent s ON a.id = s.id.assignmentId WHERE s.id.studentId = :studentId ORDER BY a.createdAt DESC")
    List<Assignment> findAssignmentsForStudent(@Param("studentId") Integer studentId);

    @EntityGraph(attributePaths = {"teacher", "schoolClass"})
    @Query("SELECT a FROM Assignment a LEFT JOIN a.schoolClass c WHERE c.school.id = :schoolId "
            + "OR (c IS NULL AND a.teacher.school.id = :schoolId)")
    Page<Assignment> findSchoolAssignments(@Param("schoolId") UUID schoolId, Pageable pageable);
}
