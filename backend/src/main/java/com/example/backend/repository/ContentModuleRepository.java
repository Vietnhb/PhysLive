package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ContentModule;
import com.example.backend.entity.Topic;

@Repository
public interface ContentModuleRepository extends JpaRepository<ContentModule, UUID> {
    @Query("select m from ContentModule m where m.topic.id = :topicId order by m.sortOrder asc")
    List<ContentModule> findByTopicIdOrderBySortOrderAsc(@Param("topicId") UUID topicId);

    Optional<ContentModule> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Optional<ContentModule> findByTopicAndSlug(Topic topic, String slug);
}
