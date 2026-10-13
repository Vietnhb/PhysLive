package com.example.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.Topic;

import jakarta.persistence.LockModeType;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    List<Topic> findByEnabledTrueOrderBySortOrderAsc();
    Optional<Topic> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Topic> findAllByOrderBySortOrderAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select topic from Topic topic where topic.id = :id")
    Optional<Topic> findByIdForReleaseApproval(@Param("id") UUID id);
}
