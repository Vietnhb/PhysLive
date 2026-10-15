package com.example.backend.repository;

import com.example.backend.entity.SupportItem;
import com.example.backend.entity.enums.SupportKind;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportItemRepository extends JpaRepository<SupportItem, UUID> {
    @EntityGraph(attributePaths = {"sender", "respondedBy"})
    List<SupportItem> findTop200ByKindOrderByCreatedAtDesc(SupportKind kind);

    @EntityGraph(attributePaths = {"sender", "respondedBy"})
    List<SupportItem> findBySenderIdOrderByCreatedAtDesc(Integer senderId);
}
