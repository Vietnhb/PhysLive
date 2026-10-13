package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.StudentActionLog;

public interface StudentActionLogRepository extends JpaRepository<StudentActionLog, UUID> {
    List<StudentActionLog> findTop200ByStudentIdOrderByOccurredAtDesc(Integer studentId);
}
