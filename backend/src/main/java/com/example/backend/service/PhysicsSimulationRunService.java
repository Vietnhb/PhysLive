package com.example.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.simulation.CreateSimulationRunRequest;
import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.dto.simulation.SnapshotResponse;
import com.example.backend.entity.Simulation;
import com.example.backend.entity.SimulationRun;
import com.example.backend.entity.SimulationRunStatus;
import com.example.backend.entity.SimulationStatus;
import com.example.backend.entity.SolverType;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.Snapshot;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.simulation.SimulationParameterPolicy;
import com.example.backend.physics.simulation.SimulationPayloadBuilder;
import com.example.backend.physics.simulation.SimulationPayloadBuilder.RunArtifacts;
import com.example.backend.physics.simulation.SimulationPayloadBuilder.SnapshotPayload;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverRegistry;
import com.example.backend.repository.SimulationRepository;
import com.example.backend.repository.SimulationRunRepository;
import com.example.backend.repository.SnapshotRepository;
import com.example.backend.repository.SolverVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhysicsSimulationRunService {

    private final SimulationRepository simulationRepository;
    private final SimulationRunRepository runRepository;
    private final SnapshotRepository snapshotRepository;
    private final SolverVersionRepository solverVersionRepository;
    private final CurrentUserService currentUserService;
    private final NumericalSolverRegistry numericalRegistry;
    private final SimulationParameterPolicy parameterPolicy;
    private final SimulationPayloadBuilder payloadBuilder;
    private final ObjectMapper objectMapper;

    @Transactional
    public SimulationRunResponse create(UUID simulationId, CreateSimulationRunRequest request) {
        User owner = currentUserService.requireCurrentUser();
        Simulation simulation = simulationRepository.findOwnedForRunCreation(simulationId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Simulation not found"));
        if (simulation.getStatus() != SimulationStatus.READY) {
            throw new ApiException(HttpStatus.CONFLICT, "Simulation is not ready for a new run");
        }

        SolverInput baseInput = readBaseInput(simulation);
        SolverInput input = parameterPolicy.apply(baseInput, request);
        NumericalSolver solver = numericalRegistry.require(input.archetype());
        SolverVersion solverVersion = resolveSolverVersion(simulation, solver);
        SimulationRun run = startRun(simulation, solverVersion, input);
        long startedNanos = System.nanoTime();

        SolverResult result;
        RunArtifacts artifacts;
        try {
            result = solver.solve(input);
            artifacts = payloadBuilder.build(input, result);
        } catch (RuntimeException exception) {
            completeFailedRun(run, exception, startedNanos);
            return toResponse(runRepository.save(run));
        }

        run.setOutputPayload(artifacts.outputPayload());
        run.setFrameCount(artifacts.snapshots().size());
        run.setStatus(SimulationRunStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        run.setExecutionTimeMs(elapsedMillis(startedNanos));
        runRepository.save(run);
        snapshotRepository.saveAll(toSnapshots(run, artifacts.snapshots()));
        if (simulation.getSceneDefinition() == null) {
            simulation.setSceneDefinition(artifacts.sceneDefinition());
            simulationRepository.save(simulation);
        }
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public SimulationRunResponse get(UUID runId) {
        return toResponse(requireOwnedRun(runId));
    }

    @Transactional(readOnly = true)
    public List<SnapshotResponse> snapshots(UUID runId) {
        SimulationRun run = requireOwnedRun(runId);
        return snapshotRepository.findBySimulationRunIdOrderBySequenceNumberAsc(run.getId()).stream()
                .map(this::toSnapshotResponse)
                .toList();
    }

    private SimulationRun requireOwnedRun(UUID runId) {
        User owner = currentUserService.requireCurrentUser();
        return runRepository.findByIdAndSimulationValidationRunSpecificationSubmissionOwner(runId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Simulation run not found"));
    }

    private SolverInput readBaseInput(Simulation simulation) {
        try {
            return objectMapper.treeToValue(simulation.getInitialParameters(), SolverInput.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored simulation parameters are invalid");
        }
    }

    private SolverVersion resolveSolverVersion(Simulation simulation, NumericalSolver solver) {
        SolverVersion validatedVersion = simulation.getValidationRun().getNumericalSolverVersion();
        if (validatedVersion.getVersion().equals(solver.version())) {
            return validatedVersion;
        }
        return solverVersionRepository.findByArchetypeAndSolverTypeAndVersion(
                solver.archetype(), SolverType.NUMERICAL, solver.version()).orElseGet(() -> {
                    SolverVersion version = new SolverVersion();
                    version.setArchetype(solver.archetype());
                    version.setSolverType(SolverType.NUMERICAL);
                    version.setVersion(solver.version());
                    version.setImplementationClass(solver.getClass().getName());
                    version.setActive(true);
                    return solverVersionRepository.save(version);
                });
    }

    private SimulationRun startRun(Simulation simulation, SolverVersion solverVersion, SolverInput input) {
        SimulationRun run = new SimulationRun();
        run.setSimulation(simulation);
        run.setSolverVersion(solverVersion);
        run.setStatus(SimulationRunStatus.RUNNING);
        run.setInputParameters(objectMapper.valueToTree(input));
        run.setFrameCount(0);
        run.setStartedAt(Instant.now());
        return runRepository.save(run);
    }

    private void completeFailedRun(SimulationRun run, RuntimeException exception, long startedNanos) {
        run.setStatus(SimulationRunStatus.FAILED);
        run.setCompletedAt(Instant.now());
        run.setExecutionTimeMs(elapsedMillis(startedNanos));
        String message = exception.getMessage();
        run.setErrorMessage(message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message);
    }

    private List<Snapshot> toSnapshots(SimulationRun run, List<SnapshotPayload> payloads) {
        List<Snapshot> snapshots = new ArrayList<>(payloads.size());
        for (SnapshotPayload payload : payloads) {
            Snapshot snapshot = new Snapshot();
            snapshot.setSimulationRun(run);
            snapshot.setSequenceNumber(payload.sequence());
            snapshot.setSimulationTime(payload.time());
            snapshot.setStatePayload(payload.state());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private SimulationRunResponse toResponse(SimulationRun run) {
        return new SimulationRunResponse(
                run.getId(),
                run.getSimulation().getId(),
                run.getSimulation().getValidationRun().getArchetype(),
                run.getStatus(),
                run.getSolverVersion().getVersion(),
                run.getInputParameters(),
                run.getOutputPayload(),
                run.getFrameCount(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getExecutionTimeMs(),
                run.getErrorMessage());
    }

    private SnapshotResponse toSnapshotResponse(Snapshot snapshot) {
        return new SnapshotResponse(snapshot.getId(), snapshot.getSequenceNumber(),
                snapshot.getSimulationTime(), snapshot.getStatePayload());
    }
}
