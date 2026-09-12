package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ContentModule;

@Repository
public interface ContentModuleRepository extends JpaRepository<ContentModule, UUID> {
    List<ContentModule> findByTopicIdOrderBySortOrderAsc(UUID topicId);
    Optional<ContentModule> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
