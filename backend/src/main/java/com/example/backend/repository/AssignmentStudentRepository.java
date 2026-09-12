package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.AssignmentStudent;
import com.example.backend.entity.AssignmentStudentId;

@Repository
public interface AssignmentStudentRepository extends JpaRepository<AssignmentStudent, AssignmentStudentId> {

    List<AssignmentStudent> findByIdAssignmentId(UUID assignmentId);

    List<AssignmentStudent> findByIdStudentId(Integer studentId);

    void deleteByIdAssignmentId(UUID assignmentId);

    boolean existsByIdAssignmentIdAndIdStudentId(UUID assignmentId, Integer studentId);
}
