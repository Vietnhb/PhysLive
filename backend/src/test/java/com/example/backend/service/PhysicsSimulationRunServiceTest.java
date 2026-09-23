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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.dto.simulation.CreateSimulationRunRequest;
import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.entity.Simulation;
import com.example.backend.entity.SimulationRun;
import com.example.backend.entity.SimulationRunStatus;
import com.example.backend.entity.SimulationStatus;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.SolverType;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.Specification;
import com.example.backend.entity.Snapshot;
import com.example.backend.entity.User;
import com.example.backend.entity.ValidationRun;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.simulation.SimulationParameterPolicy;
import com.example.backend.physics.simulation.SimulationPayloadBuilder;
import com.example.backend.physics.simulation.EndConditionResolver;
import com.example.backend.physics.simulation.SimulationPayloadBuilder.RunArtifacts;
import com.example.backend.physics.simulation.SimulationPayloadBuilder.SnapshotPayload;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverRegistry;
import com.example.backend.repository.SimulationRepository;
import com.example.backend.repository.SimulationRunRepository;
import com.example.backend.repository.SnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PhysicsSimulationRunServiceTest {

    @Mock private SimulationRepository simulationRepository;
    @Mock private SimulationRunRepository runRepository;
    @Mock private SnapshotRepository snapshotRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private NumericalSolverRegistry numericalRegistry;
    @Mock private SimulationParameterPolicy parameterPolicy;
    @Mock private SimulationPayloadBuilder payloadBuilder;
    @Mock private EndConditionResolver endConditionResolver;
    @Mock private NumericalSolver solver;

    private ObjectMapper objectMapper;
    private PhysicsSimulationRunService service;
    private User owner;
    private Simulation simulation;
    private SolverInput baseInput;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new PhysicsSimulationRunService(simulationRepository, runRepository, snapshotRepository,
                currentUserService, numericalRegistry, parameterPolicy,
                payloadBuilder, endConditionResolver, objectMapper);
        owner = new User();
        owner.setId(5);
        baseInput = new SolverInput(UUID.randomUUID(), "1.0", PhysicsArchetype.UNIFORM_MOTION,
                Map.of("velocity", decimal("2"), "duration", decimal("3")),
                Map.of("velocity", "m/s", "duration", "s"));
        simulation = simulation(baseInput);
    }

    @Test
    void reSolvesAndPersistsImmutableRunAndSnapshots() {
        SolverInput overridden = new SolverInput(baseInput.specificationId(), "1.0",
                PhysicsArchetype.UNIFORM_MOTION,
                Map.of("velocity", decimal("4"), "duration", decimal("3")),
                Map.of("velocity", "m/s", "duration", "s"));
        CreateSimulationRunRequest request = new CreateSimulationRunRequest(
                Map.of("velocity", decimal("4")), Map.of());
        SolverResult solverResult = new SolverResult(SolverMethod.NUMERICAL, "numerical-v1",
                Map.of("finalPosition", decimal("12")),
                List.of(new SimulationFrame(BigDecimal.ZERO, Map.of("position", BigDecimal.ZERO))));
        RunArtifacts artifacts = artifacts();
        prepareOwnedSimulation();
        when(parameterPolicy.apply(any(SolverInput.class), any())).thenReturn(overridden);
        when(numericalRegistry.require(overridden.archetype())).thenReturn(solver);
        when(solver.version()).thenReturn("numerical-v1");
        when(solver.solve(overridden)).thenReturn(solverResult);
        when(payloadBuilder.build(overridden, solverResult)).thenReturn(artifacts);
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(call -> {
            SimulationRun run = call.getArgument(0);
            if (run.getId() == null) run.setId(UUID.randomUUID());
            return run;
        });

        SimulationRunResponse response = service.create(simulation.getId(), request);

        assertThat(response.status()).isEqualTo(SimulationRunStatus.COMPLETED);
        assertThat(response.solverVersion()).isEqualTo("numerical-v1");
        assertThat(response.frameCount()).isEqualTo(2);
        assertThat(response.inputParameters().get("values").get("velocity").decimalValue())
                .isEqualByComparingTo("4");
        verify(snapshotRepository).saveAll(any());
        verify(simulationRepository).save(simulation);
    }

    @Test
    void blocksRunWhenLoadedSolverDoesNotMatchApprovedRelease() {
        prepareOwnedSimulation();
        when(parameterPolicy.apply(any(SolverInput.class), any())).thenReturn(baseInput);
        when(numericalRegistry.require(baseInput.archetype())).thenReturn(solver);
        when(solver.version()).thenReturn("numerical-v2");

        assertThatThrownBy(() -> service.create(simulation.getId(), null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("new release");
        verify(runRepository, never()).save(any());
    }

    @Test
    void failedSolverIsAuditedWithoutSnapshots() {
        prepareOwnedSimulation();
        when(parameterPolicy.apply(any(SolverInput.class), any())).thenReturn(baseInput);
        when(numericalRegistry.require(baseInput.archetype())).thenReturn(solver);
        when(solver.version()).thenReturn("numerical-v1");
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(call -> {
            SimulationRun run = call.getArgument(0);
            if (run.getId() == null) run.setId(UUID.randomUUID());
            return run;
        });
        when(solver.solve(baseInput)).thenThrow(new IllegalStateException("calculation failed"));

        SimulationRunResponse response = service.create(simulation.getId(), null);

        assertThat(response.status()).isEqualTo(SimulationRunStatus.FAILED);
        assertThat(response.errorMessage()).contains("calculation failed");
        assertThat(response.frameCount()).isZero();
        verify(snapshotRepository, never()).saveAll(any());
    }

    @Test
    void blocksArchivedOrUnownedSimulation() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(simulationRepository.findOwnedForRunCreation(simulation.getId(), owner))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(simulation.getId(), null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Simulation not found");

        simulation.setStatus(SimulationStatus.ARCHIVED);
        when(simulationRepository.findOwnedForRunCreation(simulation.getId(), owner))
                .thenReturn(Optional.of(simulation));
        assertThatThrownBy(() -> service.create(simulation.getId(), null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not ready");
        verify(numericalRegistry, never()).require(any());
    }

    @Test
    void replayReadsStoredRunAndSnapshotsWithoutCallingSolver() {
        SimulationRun run = new SimulationRun();
        run.setId(UUID.randomUUID());
        run.setSimulation(simulation);
        run.setSolverVersion(solverVersion("numerical-v1"));
        run.setStatus(SimulationRunStatus.COMPLETED);
        run.setInputParameters(objectMapper.valueToTree(baseInput));
        run.setOutputPayload(objectMapper.createObjectNode().put("payloadVersion", "simulation-replay-v1"));
        run.setFrameCount(1);
        Snapshot snapshot = new Snapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setSimulationRun(run);
        snapshot.setSequenceNumber(0);
        snapshot.setSimulationTime(BigDecimal.ZERO);
        snapshot.setStatePayload(objectMapper.createObjectNode().put("stored", true));
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(runRepository.findByIdAndSimulationValidationRunSpecificationSubmissionOwner(run.getId(), owner))
                .thenReturn(Optional.of(run));
        when(snapshotRepository.findBySimulationRunIdOrderBySequenceNumberAsc(run.getId()))
                .thenReturn(List.of(snapshot));
        simulation.getValidationRun().getModuleRelease().setStatus(ModuleReleaseStatus.RETIRED);

        SimulationRunResponse response = service.get(run.getId());
        var snapshots = service.snapshots(run.getId());

        assertThat(response.solverVersion()).isEqualTo("numerical-v1");
        assertThat(snapshots).singleElement().satisfies(item ->
                assertThat(item.state().get("stored").asBoolean()).isTrue());
        verify(numericalRegistry, never()).require(any());
        verify(solver, never()).solve(any());
    }

    private void prepareOwnedSimulation() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(simulationRepository.findOwnedForRunCreation(simulation.getId(), owner))
                .thenReturn(Optional.of(simulation));
    }

    private Simulation simulation(SolverInput input) {
        Specification specification = new Specification();
        specification.setId(input.specificationId());
        ValidationRun validation = new ValidationRun();
        validation.setId(UUID.randomUUID());
        validation.setSpecification(specification);
        validation.setArchetype(input.archetype());
        validation.setNumericalSolverVersion(solverVersion("numerical-v1"));
        TopicModuleRelease release = new TopicModuleRelease();
        release.setStatus(ModuleReleaseStatus.APPROVED);
        release.setNumericalSolverVersion(validation.getNumericalSolverVersion());
        validation.setModuleRelease(release);
        Simulation result = new Simulation();
        result.setId(UUID.randomUUID());
        result.setValidationRun(validation);
        result.setStatus(SimulationStatus.READY);
        result.setInitialParameters(objectMapper.valueToTree(input));
        return result;
    }

    private SolverVersion solverVersion(String version) {
        SolverVersion result = new SolverVersion();
        result.setVersion(version);
        result.setSolverType(SolverType.NUMERICAL);
        result.setArchetype(PhysicsArchetype.UNIFORM_MOTION);
        return result;
    }

    private RunArtifacts artifacts() {
        return new RunArtifacts(
                objectMapper.createObjectNode().put("payloadVersion", "simulation-replay-v1"),
                objectMapper.createObjectNode().put("coordinateSystem", "CARTESIAN_1D"),
                List.of(
                        new SnapshotPayload(0, BigDecimal.ZERO,
                                objectMapper.createObjectNode().put("position", 0)),
                        new SnapshotPayload(1, BigDecimal.ONE,
                                objectMapper.createObjectNode().put("position", 4))));
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
