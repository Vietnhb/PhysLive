package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.SchemaVersion;

@Repository
public interface SchemaVersionRepository extends JpaRepository<SchemaVersion, UUID> {
    List<SchemaVersion> findAllByLifecycleStatusAndTopicInOrderByTopicAscSchemaIdAscCreatedAtDesc(
            String lifecycleStatus, List<String> topics);
    List<SchemaVersion> findByTopic(String topic);
    List<SchemaVersion> findByLifecycleStatus(String lifecycleStatus);
    Optional<SchemaVersion> findBySchemaIdAndVersion(String schemaId, String version);
    Optional<SchemaVersion> findByVersion(String version);
}
