package com.example.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.validation.ValidationRunResponse;
import com.example.backend.entity.Specification;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.entity.User;
import com.example.backend.entity.ValidationRun;
import com.example.backend.entity.ValidationRunStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.adapter.SpecificationToSolverInputAdapter;
import com.example.backend.physics.model.PhysicsValidationStatus;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.model.ValidationResult;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverRegistry;
import com.example.backend.physics.solver.ReferenceSolver;
import com.example.backend.physics.solver.ReferenceSolverRegistry;
import com.example.backend.physics.validation.ResultComparator;
import com.example.backend.repository.SpecificationRepository;
import com.example.backend.repository.ValidationRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhysicsValidationService {

    private final SpecificationRepository specificationRepository;
    private final ValidationRunRepository validationRunRepository;
    private final CurrentUserService currentUserService;
    private final SpecificationReadinessChecker readinessChecker;
    private final SpecificationToSolverInputAdapter adapter;
    private final NumericalSolverRegistry numericalRegistry;
    private final ReferenceSolverRegistry referenceRegistry;
    private final ResultComparator comparator;
    private final ObjectMapper objectMapper;
    private final TopicModuleReleaseService releaseService;

    @Transactional
    public ValidationRunResponse validate(UUID specificationId) {
        User owner = currentUserService.requireCurrentUser();
        Specification specification = specificationRepository
                .findByIdAndSubmissionOwner(specificationId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Specification not found"));
        List<String> blockers = readinessChecker.blockers(specification);
        if (!blockers.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Specification is not ready for validation: " + String.join("; ", blockers));
        }

        SolverInput input = adapter.convert(specification);
        NumericalSolver numericalSolver = numericalRegistry.require(input.archetype());
        ReferenceSolver referenceSolver = referenceRegistry.require(input.archetype());
        TopicModuleRelease release = releaseService.requireApprovedCompatible(
                input.archetype().topicCode(), input.schemaVersion(), input.archetype(),
                numericalSolver.version(), referenceSolver.version());
        ValidationRun run = startRun(specification, input, release);
        long startedNanos = System.nanoTime();

        try {
            SolverResult numerical = numericalSolver.solve(input);
            run.setNumericalOutput(objectMapper.valueToTree(numerical));
            SolverResult reference = referenceSolver.solve(input);
            run.setReferenceOutput(objectMapper.valueToTree(reference));
            ValidationResult result = comparator.compare(input.archetype(), numerical, reference);
            run.setAbsoluteErrors(objectMapper.valueToTree(result.absoluteErrors()));
            run.setRelativeErrors(objectMapper.valueToTree(result.relativeErrors()));
            run.setTolerances(objectMapper.valueToTree(result.tolerances()));
            run.setStatus(result.status() == PhysicsValidationStatus.PASSED
                    ? ValidationRunStatus.PASSED
                    : ValidationRunStatus.FAILED);
            run.setFailureReason(result.failureReason());
        } catch (RuntimeException exception) {
            run.setStatus(ValidationRunStatus.ERROR);
            run.setFailureReason(safeFailureReason(exception));
        }

        run.setCompletedAt(Instant.now());
        run.setExecutionTimeMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
        return toResponse(validationRunRepository.save(run));
    }

    @Transactional(readOnly = true)
    public ValidationRunResponse get(UUID validationId) {
        User owner = currentUserService.requireCurrentUser();
        ValidationRun run = validationRunRepository
                .findByIdAndSpecificationSubmissionOwner(validationId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Validation run not found"));
        return toResponse(run);
    }

    private ValidationRun startRun(
            Specification specification,
            SolverInput input,
            TopicModuleRelease release) {
        ValidationRun run = new ValidationRun();
        run.setSpecification(specification);
        run.setModuleRelease(release);
        run.setSchemaVersion(release.getSchemaVersion());
        run.setNumericalSolverVersion(release.getNumericalSolverVersion());
        run.setReferenceSolverVersion(release.getReferenceSolverVersion());
        run.setArchetype(input.archetype());
        run.setStatus(ValidationRunStatus.RUNNING);
        run.setInputPayload(objectMapper.valueToTree(input));
        run.setStartedAt(Instant.now());
        return validationRunRepository.save(run);
    }

    private String safeFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
    }

    private ValidationRunResponse toResponse(ValidationRun run) {
        return new ValidationRunResponse(
                run.getId(),
                run.getSpecification().getId(),
                run.getArchetype(),
                run.getStatus(),
                run.getInputPayload(),
                run.getNumericalOutput(),
                run.getReferenceOutput(),
                run.getAbsoluteErrors(),
                run.getRelativeErrors(),
                run.getTolerances(),
                run.getSchemaVersion().getVersion(),
                run.getNumericalSolverVersion().getVersion(),
                run.getReferenceSolverVersion().getVersion(),
                run.getModuleRelease() == null ? null : run.getModuleRelease().getId(),
                run.getModuleRelease() == null ? null : run.getModuleRelease().getReleaseVersion(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getExecutionTimeMs(),
                run.getFailureReason());
    }
}
