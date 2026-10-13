package com.example.backend.service;

import java.time.ZoneOffset;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.CreateSimulationRunRequest;
import com.example.backend.dto.SimulationRunResponse;
import com.example.backend.entity.Simulation;
import com.example.backend.entity.SimulationRun;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.SimulationRepository;
import com.example.backend.repository.SimulationRunRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SimulationAttemptService {

    private final SimulationRepository simulationRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final CurrentUserService currentUserService;
    private final com.example.backend.service.school.LicenseCheckService licenses;

    @Transactional
    public SimulationRunResponse recordSimulationRun(CreateSimulationRunRequest request) {
        var actor = currentUserService.requireAnyRole("STAFF", "MANAGER");
        licenses.requireWriteAccess(actor);
        Simulation simulation = simulationRepository.findById(request.getSimulationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy mô phỏng với ID: " + request.getSimulationId()));

        requireOwner(simulation, actor);
        SimulationRun run = new SimulationRun();
        run.setSimulationId(simulation.getId());
        run.setSimulation(simulation);
        run.setDurationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0.0);
        run.setResult(request.getResult());
        run.setRunType(request.getRunType() != null ? request.getRunType() : "INTERACTIVE");
        run.setValidationPassed(request.getValidationPassed() != null ? request.getValidationPassed() : true);

        // Also update simulation's latestResult
        simulation.setLatestResult(request.getResult());
        simulationRepository.save(simulation);

        SimulationRun saved = simulationRunRepository.save(run);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SimulationRunResponse> getSimulationRuns(UUID simulationId) {
        var actor = currentUserService.requireCurrentUser();
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Simulation not found"));
        requireOwner(simulation, actor);
        return simulationRunRepository.findBySimulationIdOrderByCreatedAtDesc(simulationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void requireOwner(Simulation simulation, com.example.backend.entity.User actor) {
        if (actor.getRole() != null && "MANAGER".equalsIgnoreCase(actor.getRole().getName())) return;
        Integer ownerId = simulation.getValidationRun() != null
                ? simulation.getValidationRun().getSpecification().getSubmission().getOwner().getId() : simulation.getOwnerId();
        if (!actor.getId().equals(ownerId)) throw new ApiException(HttpStatus.NOT_FOUND, "Simulation not found");
    }

    private SimulationRunResponse toResponse(SimulationRun run) {

        return SimulationRunResponse.builder()
                .id(run.getId())
                .simulationId(run.getSimulationId())
                .durationSeconds(run.getDurationSeconds())
                .result(run.getResult())
                .runType(run.getRunType())
                .validationPassed(run.isValidationPassed())
                .createdAt(run.getCreatedAt() != null ? run.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .updatedAt(run.getUpdatedAt() != null ? run.getUpdatedAt().atOffset(ZoneOffset.UTC) : null)
                .build();
    }
}

