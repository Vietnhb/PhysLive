package com.example.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.SchemaVersion;

public interface SchemaVersionRepository extends JpaRepository<SchemaVersion, UUID> {
    Optional<SchemaVersion> findByVersion(String version);
}
