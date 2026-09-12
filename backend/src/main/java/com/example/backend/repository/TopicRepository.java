package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    List<Topic> findAllByOrderBySortOrderAsc();
    Optional<Topic> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
