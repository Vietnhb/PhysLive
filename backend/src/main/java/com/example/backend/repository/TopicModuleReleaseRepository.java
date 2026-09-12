package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.TopicModuleRelease;

@Repository
public interface TopicModuleReleaseRepository extends JpaRepository<TopicModuleRelease, UUID> {
    List<TopicModuleRelease> findByTopic(String topic);
    List<TopicModuleRelease> findByLifecycleStatus(String lifecycleStatus);
}
