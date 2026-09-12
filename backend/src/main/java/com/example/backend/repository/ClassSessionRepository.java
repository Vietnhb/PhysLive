package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ClassSession;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {

    List<ClassSession> findByTeacherIdOrderByCreatedAtDesc(Integer teacherId);

    List<ClassSession> findBySpecificationIdOrderByCreatedAtDesc(UUID specificationId);
}
