package com.example.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.simulation.CreateSimulationRunRequest;
import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.dto.simulation.SnapshotResponse;
import com.example.backend.dto.simulation.ResolvedEnd;
import com.example.backend.entity.Simulation;
import com.example.backend.entity.SimulationRun;
import com.example.backend.entity.SimulationRunStatus;
import com.example.backend.entity.SimulationStatus;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.Snapshot;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.model.SolverInput;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhysicsSimulationRunService {

    private final SimulationRepository simulationRepository;
    private final SimulationRunRepository runRepository;
    private final SnapshotRepository snapshotRepository;
    private final CurrentUserService currentUserService;
    private final NumericalSolverRegistry numericalRegistry;
    private final SimulationParameterPolicy parameterPolicy;
    private final SimulationPayloadBuilder payloadBuilder;
    private final EndConditionResolver endConditionResolver;
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
        input = withDuration(input, endConditionResolver.requestedHorizon(
                input.endCondition(), input.values().get("duration")));
        NumericalSolver solver = numericalRegistry.require(input.archetype());
        SolverVersion solverVersion = requireApprovedSolverVersion(simulation, solver);
        SimulationRun run = startRun(simulation, solverVersion, input);
        long startedNanos = System.nanoTime();

        SolverResult result;
        RunArtifacts artifacts;
        try {
            result = solver.solve(input);
            if (input.endCondition() == null) {
                artifacts = payloadBuilder.build(input, result);
            } else {
                ResolvedEnd resolvedEnd = endConditionResolver.resolve(input.endCondition(), result);
                BigDecimal horizon = input.values().get("duration");
                while (endConditionResolver.expandable(input.endCondition(), resolvedEnd, horizon)) {
                    BigDecimal maximum = maximumHorizon(input);
                    BigDecimal next = horizon.multiply(BigDecimal.TWO).min(maximum);
                    if (next.compareTo(horizon) <= 0) break;
                    input = withDuration(input, next);
                    result = solver.solve(input);
                    horizon = next;
                    resolvedEnd = endConditionResolver.resolve(input.endCondition(), result);
                }
                result = endConditionResolver.trim(result, resolvedEnd.time());
                run.setInputParameters(objectMapper.valueToTree(input));
                artifacts = payloadBuilder.build(input, result, resolvedEnd);
            }
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

    private SolverInput withDuration(SolverInput input, BigDecimal duration) {
        if (duration == null || duration.equals(input.values().get("duration"))) return input;
        Map<String, BigDecimal> values = new LinkedHashMap<>(input.values());
        values.put("duration", duration);
        return new SolverInput(input.specificationId(), input.schemaVersion(), input.archetype(),
                values, input.seriesValues(), input.units(), input.endCondition());
    }

    private BigDecimal maximumHorizon(SolverInput input) {
        BigDecimal step = input.values().getOrDefault("deltaTime", new BigDecimal("0.01"));
        return EndConditionResolver.MAX_DYNAMIC_SECONDS.min(
                step.multiply(BigDecimal.valueOf(10_000L)));
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

    private SolverVersion requireApprovedSolverVersion(Simulation simulation, NumericalSolver solver) {
        TopicModuleRelease release = simulation.getValidationRun().getModuleRelease();
        if (release == null || release.getStatus() != ModuleReleaseStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Simulation module release is not approved for new runs");
        }
        SolverVersion approvedVersion = release.getNumericalSolverVersion();
        if (approvedVersion.getArchetype() != simulation.getValidationRun().getArchetype()
                || !approvedVersion.getVersion().equals(solver.version())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "The approved numerical solver implementation is not available; create and approve a new release");
        }
        return approvedVersion;
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
