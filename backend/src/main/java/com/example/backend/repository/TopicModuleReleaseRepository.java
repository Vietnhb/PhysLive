package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.Topic;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.physics.model.PhysicsArchetype;

import jakarta.persistence.LockModeType;

public interface TopicModuleReleaseRepository extends JpaRepository<TopicModuleRelease, UUID> {
    Optional<TopicModuleRelease> findByTopicAndReleaseVersion(Topic topic, String releaseVersion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select release from TopicModuleRelease release where release.id = :id")
    Optional<TopicModuleRelease> findByIdForUpdate(@Param("id") UUID id);

    Optional<TopicModuleRelease> findFirstByTopicAndStatusOrderByCreatedAtDesc(
            Topic topic, ModuleReleaseStatus status);

    boolean existsByTopicAndStatusAndNumericalSolverVersionArchetype(
            Topic topic, ModuleReleaseStatus status, PhysicsArchetype archetype);

    @Query("""
            select release
            from TopicModuleRelease release
            join release.topic topic
            join release.schemaVersion schemaVersion
            join release.numericalSolverVersion numerical
            join release.referenceSolverVersion reference
            where lower(topic.slug) = lower(:topicCode)
              and release.status = com.example.backend.entity.ModuleReleaseStatus.APPROVED
              and schemaVersion.version = :schemaVersion
              and numerical.archetype = :archetype
              and numerical.version = :numericalVersion
              and reference.archetype = :archetype
              and reference.version = :referenceVersion
            order by release.approvedAt desc
            """)
    java.util.List<TopicModuleRelease> findApprovedCompatibleReleases(
            @Param("topicCode") String topicCode,
            @Param("schemaVersion") String schemaVersion,
            @Param("archetype") PhysicsArchetype archetype,
            @Param("numericalVersion") String numericalVersion,
            @Param("referenceVersion") String referenceVersion);
}
