package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ContentModule;
import com.example.backend.entity.GradeLevel;

@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, UUID> {
    @Query("select g from GradeLevel g where g.module.id = :moduleId order by g.sortOrder asc")
    List<GradeLevel> findByModuleIdOrderBySortOrderAsc(@Param("moduleId") UUID moduleId);

    Optional<GradeLevel> findByModuleAndName(ContentModule module, String name);
}
