package com.example.backend.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.CreateModuleReleaseRequest;
import com.example.backend.dto.CreateSchemaVersionRequest;
import com.example.backend.dto.CreateSolverVersionRequest;
import com.example.backend.dto.SchemaVersionResponse;
import com.example.backend.dto.SolverVersionResponse;
import com.example.backend.dto.TopicModuleReleaseResponse;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.SchemaVersion;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.Topic;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.SchemaVersionRepository;
import com.example.backend.repository.SolverVersionRepository;
import com.example.backend.repository.TopicModuleReleaseRepository;
import com.example.backend.repository.TopicRepository;

import lombok.AllArgsConstructor;

/**
 * Service for Physics Content Reviewer to define, version, approve and retire
 * schema models, solver contracts, and module releases.
 */
@Service
@AllArgsConstructor
public class ReviewerSchemaService {

    private final SchemaVersionRepository schemaVersionRepository;
    private final SolverVersionRepository solverVersionRepository;
    private final TopicModuleReleaseRepository moduleReleaseRepository;
    private final TopicRepository topicRepository;

    // ------------------------------------------------------------------ //
    // Schema Versions
    // ------------------------------------------------------------------ //

    public List<SchemaVersionResponse> getSchemaVersions(String topic, String status) {
        List<SchemaVersion> list;
        if (topic != null && !topic.trim().isEmpty()) {
            list = schemaVersionRepository.findByTopic(topic.trim().toLowerCase());
        } else if (status != null && !status.trim().isEmpty()) {
            list = schemaVersionRepository.findByLifecycleStatus(status.trim().toUpperCase());
        } else {
            list = schemaVersionRepository.findAll();
        }
        return list.stream().map(this::toSchemaResponse).toList();
    }

    public SchemaVersionResponse getSchemaVersionById(UUID id) {
        SchemaVersion entity = schemaVersionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy schema version với id: " + id));
        return toSchemaResponse(entity);
    }

    @Transactional
    public SchemaVersionResponse createSchemaVersion(CreateSchemaVersionRequest request) {
        if (schemaVersionRepository.findBySchemaIdAndVersion(request.getSchemaId().trim(), request.getVersion().trim()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Schema version đã tồn tại cho schema_id: " + request.getSchemaId() + " version: " + request.getVersion());
        }

        SchemaVersion entity = new SchemaVersion();
        entity.setSchemaId(request.getSchemaId().trim());
        entity.setTopic(request.getTopic().trim().toLowerCase());
        entity.setVersion(request.getVersion().trim());
        entity.setName(request.getName().trim());
        entity.setDefinition(request.getDefinition());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        entity.setActive(true);
        entity.setLifecycleStatus("DRAFT");

        schemaVersionRepository.save(entity);
        return toSchemaResponse(entity);
    }

    @Transactional
    public SchemaVersionResponse updateSchemaLifecycle(UUID id, String status) {
        SchemaVersion entity = schemaVersionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy schema version với id: " + id));

        validateLifecycleStatus(status);
        entity.setLifecycleStatus(status.trim().toUpperCase());
        schemaVersionRepository.save(entity);
        return toSchemaResponse(entity);
    }

    // ------------------------------------------------------------------ //
    // Solver Versions
    // ------------------------------------------------------------------ //

    public List<SolverVersionResponse> getSolverVersions(String schemaId, String status) {
        List<SolverVersion> list;
        if (schemaId != null && !schemaId.trim().isEmpty()) {
            list = solverVersionRepository.findBySchemaId(schemaId.trim());
        } else if (status != null && !status.trim().isEmpty()) {
            list = solverVersionRepository.findByLifecycleStatus(status.trim().toUpperCase());
        } else {
            list = solverVersionRepository.findAll();
        }
        return list.stream().map(this::toSolverResponse).toList();
    }

    @Transactional
    public SolverVersionResponse createSolverVersion(CreateSolverVersionRequest request) {
        SolverVersion entity = new SolverVersion();
        entity.setSolverId(request.getSolverId().trim());
        entity.setSchemaId(request.getSchemaId().trim());
        entity.setVersion(request.getVersion().trim());
        entity.setOutputDefinition(request.getOutputDefinition());
        entity.setLifecycleStatus("DRAFT");
        entity.setActive(true);

        solverVersionRepository.save(entity);
        return toSolverResponse(entity);
    }

    @Transactional
    public SolverVersionResponse updateSolverLifecycle(UUID id, String status) {
        SolverVersion entity = solverVersionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy solver version với id: " + id));

        validateLifecycleStatus(status);
        entity.setLifecycleStatus(status.trim().toUpperCase());
        solverVersionRepository.save(entity);
        return toSolverResponse(entity);
    }

    // ------------------------------------------------------------------ //
    // Topic Module Releases
    // ------------------------------------------------------------------ //

    public List<TopicModuleReleaseResponse> getTopicModuleReleases(String topic) {
        List<TopicModuleRelease> list;
        if (topic != null && !topic.trim().isEmpty()) {
            list = moduleReleaseRepository.findByTopic_SlugIgnoreCase(topic.trim().toLowerCase());
        } else {
            list = moduleReleaseRepository.findAll();
        }
        return list.stream().map(this::toReleaseResponse).toList();
    }

    @Transactional
    public TopicModuleReleaseResponse createModuleRelease(CreateModuleReleaseRequest request) {
        String topicSlug = request.getTopic().trim().toLowerCase();
        Topic topic = topicRepository.findBySlug(topicSlug)
                .orElseGet(() -> {
                    Topic t = new Topic();
                    t.setName(request.getTopic().trim());
                    t.setSlug(topicSlug);
                    t.setEnabled(true);
                    return topicRepository.save(t);
                });

        TopicModuleRelease entity = new TopicModuleRelease();
        entity.setTopic(topic);
        entity.setReleaseVersion(request.getSchemaVersion() != null ? request.getSchemaVersion().trim() : "v1.0.0");
        entity.setStatus(ModuleReleaseStatus.DRAFT);
        entity.setCreatedBy(1);

        schemaVersionRepository.findByVersion(request.getSchemaVersion().trim())
                .ifPresent(entity::setSchemaVersion);

        moduleReleaseRepository.save(entity);
        return toReleaseResponse(entity);
    }

    @Transactional
    public TopicModuleReleaseResponse updateReleaseLifecycle(UUID id, String status) {
        TopicModuleRelease entity = moduleReleaseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy module release với id: " + id));

        validateLifecycleStatus(status);
        entity.setStatus(ModuleReleaseStatus.valueOf(status.trim().toUpperCase()));
        if (entity.getStatus() == ModuleReleaseStatus.APPROVED) {
            entity.setApprovedAt(Instant.now());
        } else if (entity.getStatus() == ModuleReleaseStatus.RETIRED) {
            entity.setRetiredAt(Instant.now());
        }
        moduleReleaseRepository.save(entity);
        return toReleaseResponse(entity);
    }

    // ------------------------------------------------------------------ //
    // Helper
    // ------------------------------------------------------------------ //

    private void validateLifecycleStatus(String status) {
        if (status == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái vòng đời không được để trống");
        }
        String upper = status.trim().toUpperCase();
        if (!upper.equals("DRAFT") && !upper.equals("APPROVED") && !upper.equals("RETIRED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status + ". Phải là DRAFT, APPROVED hoặc RETIRED");
        }
    }

    private SchemaVersionResponse toSchemaResponse(SchemaVersion entity) {
        return new SchemaVersionResponse(
                entity.getId(),
                entity.getSchemaId(),
                entity.getTopic(),
                entity.getVersion(),
                entity.getName(),
                entity.getDefinitionAsMap(),
                entity.isEnabled(),
                entity.getLifecycleStatus(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }

    private SolverVersionResponse toSolverResponse(SolverVersion entity) {
        return new SolverVersionResponse(
                entity.getId(),
                entity.getSolverId(),
                entity.getSchemaId(),
                entity.getVersion(),
                entity.getOutputDefinitionAsMap(),
                entity.getLifecycleStatus(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }

    private TopicModuleReleaseResponse toReleaseResponse(TopicModuleRelease entity) {
        return new TopicModuleReleaseResponse(
                entity.getId(),
                entity.getTopic() != null ? entity.getTopic().getSlug() : null,
                entity.getModuleName() != null ? entity.getModuleName() : (entity.getTopic() != null ? entity.getTopic().getName() : null),
                entity.getSchemaVersion() != null ? entity.getSchemaVersion().getSchemaId() : entity.getSchemaId(),
                entity.getSchemaVersion() != null ? entity.getSchemaVersion().getVersion() : entity.getReleaseVersion(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                entity.getUpdatedAt() != null ? entity.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    }
}
