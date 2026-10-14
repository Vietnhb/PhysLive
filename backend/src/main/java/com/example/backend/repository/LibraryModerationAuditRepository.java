package com.example.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.LibraryModerationAudit;

public interface LibraryModerationAuditRepository extends JpaRepository<LibraryModerationAudit, UUID> {
    List<LibraryModerationAudit> findTop100ByLibraryItemIdOrderByDecidedAtDesc(UUID itemId);
}
