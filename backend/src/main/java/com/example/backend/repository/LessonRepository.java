package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    @Query("select l from Lesson l where l.level.id = :levelId order by l.sortOrder asc")
    List<Lesson> findByLevelIdOrderBySortOrderAsc(@Param("levelId") UUID levelId);
}
