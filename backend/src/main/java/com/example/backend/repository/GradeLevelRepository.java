package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.GradeLevel;

@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, UUID> {
    List<GradeLevel> findByModuleIdOrderBySortOrderAsc(UUID moduleId);
}
