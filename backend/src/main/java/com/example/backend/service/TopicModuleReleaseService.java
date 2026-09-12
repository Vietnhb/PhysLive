package com.example.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.backend.dto.release.CreateTopicModuleReleaseRequest;
import com.example.backend.dto.release.TopicModuleReleaseResponse;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.SchemaVersion;
import com.example.backend.entity.SolverType;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.Topic;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverRegistry;
import com.example.backend.physics.solver.ReferenceSolver;
import com.example.backend.physics.solver.ReferenceSolverRegistry;
import com.example.backend.repository.SchemaVersionRepository;
import com.example.backend.repository.SolverVersionRepository;
import com.example.backend.repository.TopicModuleReleaseRepository;
import com.example.backend.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopicModuleReleaseService {

    private final TopicModuleReleaseRepository releaseRepository;
    private final TopicRepository topicRepository;
    private final SchemaVersionRepository schemaVersionRepository;
    private final SolverVersionRepository solverVersionRepository;
    private final CurrentUserService currentUserService;
    private final NumericalSolverRegistry numericalRegistry;
    private final ReferenceSolverRegistry referenceRegistry;

    @Transactional
    public TopicModuleReleaseResponse createDraft(CreateTopicModuleReleaseRequest request) {
        User actor = currentUserService.requireAnyRole("REVIEWER", "ADMIN");
        validateRequest(request);
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (releaseRepository.findByTopicAndReleaseVersion(topic, request.releaseVersion().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Release version already exists for this topic");
        }
        NumericalSolver numericalImplementation = numericalRegistry.require(request.archetype());
        ReferenceSolver referenceImplementation = referenceRegistry.require(request.archetype());
        SchemaVersion schema = resolveSchemaVersion(request.schemaVersion().trim());
        SolverVersion numerical = resolveSolverVersion(
                request.archetype(), SolverType.NUMERICAL, numericalImplementation.version(),
                numericalImplementation.getClass().getName());
        SolverVersion reference = resolveSolverVersion(
                request.archetype(), SolverType.REFERENCE, referenceImplementation.version(),
                referenceImplementation.getClass().getName());
        validateBindings(topic, numerical, reference);

        TopicModuleRelease release = new TopicModuleRelease();
        release.setTopic(topic);
        release.setReleaseVersion(request.releaseVersion().trim());
        release.setSchemaVersion(schema);
        release.setNumericalSolverVersion(numerical);
        release.setReferenceSolverVersion(reference);
        release.setStatus(ModuleReleaseStatus.DRAFT);
        release.setCreatedBy(actor.getId());
        return toResponse(releaseRepository.save(release));
    }

    @Transactional
    public TopicModuleReleaseResponse approve(UUID releaseId) {
        User actor = currentUserService.requireAnyRole("REVIEWER", "ADMIN");
        TopicModuleRelease release = requireReleaseForUpdate(releaseId);
        requireStatus(release, ModuleReleaseStatus.DRAFT, "Only a DRAFT release can be approved");
        validateApprovable(release);
        topicRepository.findByIdForReleaseApproval(release.getTopic().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Topic not found"));
        PhysicsArchetype archetype = release.getNumericalSolverVersion().getArchetype();
        if (releaseRepository.existsByTopicAndStatusAndNumericalSolverVersionArchetype(
                release.getTopic(), ModuleReleaseStatus.APPROVED, archetype)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Retire the currently approved release for this topic and archetype first");
        }
        release.setStatus(ModuleReleaseStatus.APPROVED);
        release.setApprovedAt(Instant.now());
        release.setApprovedBy(actor.getId());
        return toResponse(releaseRepository.save(release));
    }

    @Transactional
    public TopicModuleReleaseResponse retire(UUID releaseId) {
        User actor = currentUserService.requireAnyRole("ADMIN");
        TopicModuleRelease release = requireReleaseForUpdate(releaseId);
        requireStatus(release, ModuleReleaseStatus.APPROVED, "Only an APPROVED release can be retired");
        release.setStatus(ModuleReleaseStatus.RETIRED);
        release.setRetiredAt(Instant.now());
        release.setRetiredBy(actor.getId());
        return toResponse(releaseRepository.save(release));
    }

    @Transactional(readOnly = true)
    public TopicModuleReleaseResponse get(UUID releaseId) {
        currentUserService.requireAnyRole("REVIEWER", "ADMIN");
        return toResponse(requireRelease(releaseId));
    }

    @Transactional(readOnly = true)
    public TopicModuleRelease requireApprovedCompatible(
            String topicCode,
            String schemaVersion,
            PhysicsArchetype archetype,
            String numericalVersion,
            String referenceVersion) {
        List<TopicModuleRelease> matches = releaseRepository.findApprovedCompatibleReleases(
                topicCode, schemaVersion, archetype, numericalVersion, referenceVersion);
        if (matches.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No approved module release matches the schema and solver versions");
        }
        return matches.get(0);
    }

    private void validateRequest(CreateTopicModuleReleaseRequest request) {
        if (request == null || request.topicId() == null || request.archetype() == null
                || !StringUtils.hasText(request.schemaVersion())
                || !StringUtils.hasText(request.releaseVersion())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Topic, release version, schema version and archetype are required");
        }
        if (request.releaseVersion().trim().length() > 32) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Release version must not exceed 32 characters");
        }
        if (request.schemaVersion().trim().length() > 32) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Schema version must not exceed 32 characters");
        }
    }

    private SchemaVersion resolveSchemaVersion(String version) {
        return schemaVersionRepository.findByVersion(version).orElseGet(() -> {
            SchemaVersion schema = new SchemaVersion();
            schema.setVersion(version);
            schema.setDescription("Registered for a topic module release");
            schema.setActive(true);
            return schemaVersionRepository.save(schema);
        });
    }

    private SolverVersion resolveSolverVersion(
            PhysicsArchetype archetype,
            SolverType type,
            String version,
            String implementationClass) {
        return solverVersionRepository.findByArchetypeAndSolverTypeAndVersion(archetype, type, version)
                .orElseGet(() -> {
                    SolverVersion solver = new SolverVersion();
                    solver.setArchetype(archetype);
                    solver.setSolverType(type);
                    solver.setVersion(version);
                    solver.setImplementationClass(implementationClass);
                    solver.setActive(true);
                    return solverVersionRepository.save(solver);
                });
    }

    private void validateBindings(Topic topic, SolverVersion numerical, SolverVersion reference) {
        if (numerical.getSolverType() != SolverType.NUMERICAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Numerical solver version has the wrong type");
        }
        if (reference.getSolverType() != SolverType.REFERENCE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reference solver version has the wrong type");
        }
        if (numerical.getArchetype() != reference.getArchetype()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solver versions must use the same archetype");
        }
        PhysicsArchetype archetype = numerical.getArchetype();
        if (!archetype.belongsTo(topic.getSlug()) && !archetype.belongsTo(topic.getName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solver archetype does not belong to the topic");
        }
    }

    private void validateApprovable(TopicModuleRelease release) {
        validateBindings(release.getTopic(), release.getNumericalSolverVersion(),
                release.getReferenceSolverVersion());
        if (!release.getTopic().isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "Topic must be enabled before approval");
        }
        if (!release.getSchemaVersion().isActive()
                || !release.getNumericalSolverVersion().isActive()
                || !release.getReferenceSolverVersion().isActive()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Schema and solver versions must be active before approval");
        }
    }

    private TopicModuleRelease requireRelease(UUID releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Module release not found"));
    }

    private TopicModuleRelease requireReleaseForUpdate(UUID releaseId) {
        return releaseRepository.findByIdForUpdate(releaseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Module release not found"));
    }

    private void requireStatus(
            TopicModuleRelease release,
            ModuleReleaseStatus expected,
            String message) {
        if (release.getStatus() != expected) {
            throw new ApiException(HttpStatus.CONFLICT, message);
        }
    }

    private TopicModuleReleaseResponse toResponse(TopicModuleRelease release) {
        return new TopicModuleReleaseResponse(
                release.getId(), release.getTopic().getId(), release.getTopic().getSlug(),
                release.getNumericalSolverVersion().getArchetype(), release.getReleaseVersion(),
                release.getSchemaVersion().getVersion(), release.getNumericalSolverVersion().getVersion(),
                release.getReferenceSolverVersion().getVersion(), release.getStatus(),
                release.getCreatedBy(), release.getApprovedBy(), release.getRetiredBy(),
                release.getCreatedAt(), release.getUpdatedAt(), release.getApprovedAt(), release.getRetiredAt());
    }
}
