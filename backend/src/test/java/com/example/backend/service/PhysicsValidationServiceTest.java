package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.dto.validation.ValidationRunResponse;
import com.example.backend.entity.ProblemSubmission;
import com.example.backend.entity.SchemaVersion;
import com.example.backend.entity.SolverType;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.Specification;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.entity.User;
import com.example.backend.entity.ValidationRun;
import com.example.backend.entity.ValidationRunStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.adapter.SpecificationToSolverInputAdapter;
import com.example.backend.physics.model.ComparisonTolerance;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.PhysicsValidationStatus;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
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

@ExtendWith(MockitoExtension.class)
class PhysicsValidationServiceTest {

    @Mock private SpecificationRepository specificationRepository;
    @Mock private ValidationRunRepository validationRunRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private SpecificationReadinessChecker readinessChecker;
    @Mock private SpecificationToSolverInputAdapter adapter;
    @Mock private NumericalSolverRegistry numericalRegistry;
    @Mock private ReferenceSolverRegistry referenceRegistry;
    @Mock private ResultComparator comparator;
    @Mock private NumericalSolver numericalSolver;
    @Mock private ReferenceSolver referenceSolver;
    @Mock private TopicModuleReleaseService releaseService;

    private PhysicsValidationService service;
    private User owner;
    private Specification specification;
    private SolverInput input;
    private TopicModuleRelease release;

    @BeforeEach
    void setUp() {
        service = new PhysicsValidationService(specificationRepository, validationRunRepository,
                currentUserService, readinessChecker, adapter, numericalRegistry, referenceRegistry,
                comparator, new ObjectMapper(), releaseService);
        owner = new User();
        owner.setId(7);
        ProblemSubmission submission = new ProblemSubmission();
        submission.setOwner(owner);
        specification = new Specification();
        specification.setId(UUID.randomUUID());
        specification.setSubmission(submission);
        input = new SolverInput(specification.getId(), "1.0", PhysicsArchetype.UNIFORM_MOTION,
                Map.of("velocity", decimal("2"), "duration", decimal("3")),
                Map.of("velocity", "m/s", "duration", "s"));
        release = release();
    }

    @Test
    void refusesToRunWhenSpecificationIsNotReady() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(specificationRepository.findByIdAndSubmissionOwner(specification.getId(), owner))
                .thenReturn(Optional.of(specification));
        when(readinessChecker.blockers(specification)).thenReturn(List.of("Missing duration"));

        assertThatThrownBy(() -> service.validate(specification.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not ready");
        verify(numericalRegistry, never()).require(any());
        verify(validationRunRepository, never()).save(any());
    }

    @Test
    void runsBothSolversAndPersistsPassedTrace() {
        prepareRunnableValidation();
        SolverResult numerical = scalarResult(SolverMethod.NUMERICAL, "numerical-v1", "6.0000000001");
        SolverResult reference = scalarResult(SolverMethod.REFERENCE, "reference-v1", "6");
        ValidationResult comparison = new ValidationResult(PhysicsValidationStatus.PASSED,
                numerical, reference, Map.of("finalPosition", decimal("1E-10")),
                Map.of("finalPosition", decimal("1.6666666667E-11")),
                Map.of("finalPosition", new ComparisonTolerance(decimal("1E-9"), decimal("1E-9"))), null);
        when(numericalSolver.solve(input)).thenReturn(numerical);
        when(referenceSolver.solve(input)).thenReturn(reference);
        when(comparator.compare(input.archetype(), numerical, reference)).thenReturn(comparison);

        ValidationRunResponse response = service.validate(specification.getId());

        assertThat(response.status()).isEqualTo(ValidationRunStatus.PASSED);
        assertThat(response.numericalOutput().get("solverVersion").asText()).isEqualTo("numerical-v1");
        assertThat(response.referenceOutput().get("solverVersion").asText()).isEqualTo("reference-v1");
        assertThat(response.absoluteErrors().get("finalPosition").decimalValue())
                .isEqualByComparingTo("1E-10");
        assertThat(response.executionTimeMs()).isNotNegative();
        verify(numericalSolver).solve(input);
        verify(referenceSolver).solve(input);
    }

    @Test
    void persistsErrorWhenSolverThrows() {
        prepareRunnableValidation();
        when(numericalSolver.solve(input)).thenThrow(new IllegalStateException("solver broke"));

        ValidationRunResponse response = service.validate(specification.getId());

        assertThat(response.status()).isEqualTo(ValidationRunStatus.ERROR);
        assertThat(response.failureReason()).contains("solver broke");
        verify(referenceSolver, never()).solve(any());
    }

    @Test
    void refusesOfficialValidationWithoutCompatibleApprovedRelease() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(specificationRepository.findByIdAndSubmissionOwner(specification.getId(), owner))
                .thenReturn(Optional.of(specification));
        when(readinessChecker.blockers(specification)).thenReturn(List.of());
        when(adapter.convert(specification)).thenReturn(input);
        when(numericalRegistry.require(input.archetype())).thenReturn(numericalSolver);
        when(referenceRegistry.require(input.archetype())).thenReturn(referenceSolver);
        when(numericalSolver.version()).thenReturn("numerical-v1");
        when(referenceSolver.version()).thenReturn("reference-v1");
        when(releaseService.requireApprovedCompatible(
                "KINEMATICS", "1.0", PhysicsArchetype.UNIFORM_MOTION,
                "numerical-v1", "reference-v1"))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                        "No approved module release"));

        assertThatThrownBy(() -> service.validate(specification.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No approved");
        verify(validationRunRepository, never()).save(any());
    }

    @Test
    void getUsesOwnerScopedRepositoryQuery() {
        ValidationRun run = runWithVersions();
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(validationRunRepository.findByIdAndSpecificationSubmissionOwner(run.getId(), owner))
                .thenReturn(Optional.of(run));

        ValidationRunResponse response = service.get(run.getId());

        assertThat(response.id()).isEqualTo(run.getId());
        verify(validationRunRepository).findByIdAndSpecificationSubmissionOwner(run.getId(), owner);
    }

    private void prepareRunnableValidation() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(specificationRepository.findByIdAndSubmissionOwner(specification.getId(), owner))
                .thenReturn(Optional.of(specification));
        when(readinessChecker.blockers(specification)).thenReturn(List.of());
        when(adapter.convert(specification)).thenReturn(input);
        when(numericalRegistry.require(input.archetype())).thenReturn(numericalSolver);
        when(referenceRegistry.require(input.archetype())).thenReturn(referenceSolver);
        when(numericalSolver.version()).thenReturn("numerical-v1");
        when(referenceSolver.version()).thenReturn("reference-v1");
        when(releaseService.requireApprovedCompatible(
                "KINEMATICS", "1.0", PhysicsArchetype.UNIFORM_MOTION,
                "numerical-v1", "reference-v1")).thenReturn(release);
        when(validationRunRepository.save(any(ValidationRun.class))).thenAnswer(call -> {
            ValidationRun run = call.getArgument(0);
            if (run.getId() == null) {
                run.setId(UUID.randomUUID());
            }
            return run;
        });
    }

    private ValidationRun runWithVersions() {
        ValidationRun run = new ValidationRun();
        run.setId(UUID.randomUUID());
        run.setSpecification(specification);
        run.setArchetype(PhysicsArchetype.UNIFORM_MOTION);
        run.setStatus(ValidationRunStatus.PASSED);
        SchemaVersion schema = new SchemaVersion();
        schema.setVersion("1.0");
        run.setSchemaVersion(schema);
        SolverVersion numerical = version(SolverType.NUMERICAL, "numerical-v1");
        SolverVersion reference = version(SolverType.REFERENCE, "reference-v1");
        run.setNumericalSolverVersion(numerical);
        run.setReferenceSolverVersion(reference);
        run.setModuleRelease(release);
        return run;
    }

    private TopicModuleRelease release() {
        TopicModuleRelease result = new TopicModuleRelease();
        result.setId(UUID.randomUUID());
        result.setReleaseVersion("kinematics-uniform-v1");
        SchemaVersion schema = new SchemaVersion();
        schema.setVersion("1.0");
        result.setSchemaVersion(schema);
        result.setNumericalSolverVersion(version(SolverType.NUMERICAL, "numerical-v1"));
        result.setReferenceSolverVersion(version(SolverType.REFERENCE, "reference-v1"));
        return result;
    }

    private SolverVersion version(SolverType type, String value) {
        SolverVersion version = new SolverVersion();
        version.setSolverType(type);
        version.setVersion(value);
        return version;
    }

    private SolverResult scalarResult(SolverMethod method, String version, String position) {
        return new SolverResult(method, version, Map.of("finalPosition", decimal(position)), List.of());
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
