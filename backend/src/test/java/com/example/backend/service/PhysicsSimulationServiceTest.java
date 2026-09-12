package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.entity.ProblemSubmission;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.entity.Simulation;
import com.example.backend.entity.SimulationStatus;
import com.example.backend.entity.Specification;
import com.example.backend.entity.User;
import com.example.backend.entity.ValidationRun;
import com.example.backend.entity.ValidationRunStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.repository.SimulationRepository;
import com.example.backend.repository.ValidationRunRepository;
import com.example.backend.service.PhysicsSimulationService.CreationResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PhysicsSimulationServiceTest {

    @Mock private ValidationRunRepository validationRunRepository;
    @Mock private SimulationRepository simulationRepository;
    @Mock private CurrentUserService currentUserService;

    private PhysicsSimulationService service;
    private User owner;
    private ValidationRun validation;

    @BeforeEach
    void setUp() {
        service = new PhysicsSimulationService(validationRunRepository, simulationRepository, currentUserService);
        owner = new User();
        owner.setId(7);
        ProblemSubmission submission = new ProblemSubmission();
        submission.setOwner(owner);
        Specification specification = new Specification();
        specification.setId(UUID.randomUUID());
        specification.setSubmission(submission);
        validation = new ValidationRun();
        validation.setId(UUID.randomUUID());
        validation.setSpecification(specification);
        validation.setArchetype(PhysicsArchetype.UNIFORM_MOTION);
        validation.setInputPayload(new ObjectMapper().createObjectNode().put("duration", 3));
        TopicModuleRelease release = new TopicModuleRelease();
        release.setStatus(ModuleReleaseStatus.APPROVED);
        validation.setModuleRelease(release);
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
    }

    @Test
    void createsReadySimulationForPassedValidation() {
        validation.setStatus(ValidationRunStatus.PASSED);
        when(validationRunRepository.findOwnedForSimulationGate(validation.getId(), owner))
                .thenReturn(Optional.of(validation));
        when(simulationRepository.findByValidationRunId(validation.getId())).thenReturn(Optional.empty());
        when(simulationRepository.save(any(Simulation.class))).thenAnswer(call -> {
            Simulation simulation = call.getArgument(0);
            simulation.setId(UUID.randomUUID());
            return simulation;
        });

        CreationResult result = service.createFromValidation(validation.getId());

        assertThat(result.created()).isTrue();
        assertThat(result.simulation().status()).isEqualTo(SimulationStatus.READY);
        assertThat(result.simulation().validationRunId()).isEqualTo(validation.getId());
        assertThat(result.simulation().initialParameters().get("duration").asInt()).isEqualTo(3);
        ArgumentCaptor<Simulation> captor = ArgumentCaptor.forClass(Simulation.class);
        verify(simulationRepository).save(captor.capture());
        assertThat(captor.getValue().getSceneDefinition()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = ValidationRunStatus.class, names = {"FAILED", "ERROR"})
    void blocksFailedAndErrorValidations(ValidationRunStatus status) {
        validation.setStatus(status);
        when(validationRunRepository.findOwnedForSimulationGate(validation.getId(), owner))
                .thenReturn(Optional.of(validation));

        assertThatThrownBy(() -> service.createFromValidation(validation.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("simulation creation is not allowed");
        verify(simulationRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ValidationRunStatus.class, names = {"PENDING", "RUNNING"})
    void blocksUnfinishedValidations(ValidationRunStatus status) {
        validation.setStatus(status);
        when(validationRunRepository.findOwnedForSimulationGate(validation.getId(), owner))
                .thenReturn(Optional.of(validation));

        assertThatThrownBy(() -> service.createFromValidation(validation.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("has not completed");
        verify(simulationRepository, never()).save(any());
    }

    @Test
    void hidesValidationOwnedByAnotherUser() {
        when(validationRunRepository.findOwnedForSimulationGate(validation.getId(), owner))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFromValidation(validation.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("Validation run not found");
        verify(simulationRepository, never()).findByValidationRunId(any());
    }

    @Test
    void repeatedRequestReturnsExistingSimulationWithoutCreatingAnother() {
        validation.setStatus(ValidationRunStatus.PASSED);
        Simulation existing = simulation(validation);
        when(validationRunRepository.findOwnedForSimulationGate(validation.getId(), owner))
                .thenReturn(Optional.of(validation));
        when(simulationRepository.findByValidationRunId(validation.getId())).thenReturn(Optional.of(existing));

        CreationResult result = service.createFromValidation(validation.getId());

        assertThat(result.created()).isFalse();
        assertThat(result.simulation().id()).isEqualTo(existing.getId());
        verify(simulationRepository, never()).save(any());
    }

    @Test
    void blocksPassedValidationWhoseReleaseWasRetired() {
        validation.setStatus(ValidationRunStatus.PASSED);
        validation.getModuleRelease().setStatus(ModuleReleaseStatus.RETIRED);
        when(validationRunRepository.findOwnedForSimulationGate(validation.getId(), owner))
                .thenReturn(Optional.of(validation));

        assertThatThrownBy(() -> service.createFromValidation(validation.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not approved");
        verify(simulationRepository, never()).save(any());
    }

    @Test
    void readsSimulationUsingOwnerScopedQuery() {
        Simulation existing = simulation(validation);
        when(simulationRepository.findByIdAndValidationRunSpecificationSubmissionOwner(existing.getId(), owner))
                .thenReturn(Optional.of(existing));

        assertThat(service.get(existing.getId()).id()).isEqualTo(existing.getId());
        verify(simulationRepository)
                .findByIdAndValidationRunSpecificationSubmissionOwner(existing.getId(), owner);
    }

    private Simulation simulation(ValidationRun sourceValidation) {
        Simulation simulation = new Simulation();
        simulation.setId(UUID.randomUUID());
        simulation.setValidationRun(sourceValidation);
        simulation.setStatus(SimulationStatus.READY);
        simulation.setInitialParameters(sourceValidation.getInputPayload());
        return simulation;
    }
}
