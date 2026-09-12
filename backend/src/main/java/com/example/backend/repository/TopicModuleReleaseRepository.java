package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.Topic;
import com.example.backend.entity.TopicModuleRelease;

public interface TopicModuleReleaseRepository extends JpaRepository<TopicModuleRelease, UUID> {
    Optional<TopicModuleRelease> findByTopicAndReleaseVersion(Topic topic, String releaseVersion);

    Optional<TopicModuleRelease> findFirstByTopicAndStatusOrderByCreatedAtDesc(
            Topic topic, ModuleReleaseStatus status);
}
