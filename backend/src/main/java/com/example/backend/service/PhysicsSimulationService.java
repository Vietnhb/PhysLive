package com.example.backend.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.simulation.SimulationResponse;
import com.example.backend.entity.Simulation;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.SimulationStatus;
import com.example.backend.entity.User;
import com.example.backend.entity.ValidationRun;
import com.example.backend.entity.ValidationRunStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.SimulationRepository;
import com.example.backend.repository.ValidationRunRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhysicsSimulationService {

    private final ValidationRunRepository validationRunRepository;
    private final SimulationRepository simulationRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public CreationResult createFromValidation(UUID validationRunId) {
        User owner = currentUserService.requireCurrentUser();
        ValidationRun validation = validationRunRepository
                .findOwnedForSimulationGate(validationRunId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Validation run not found"));
        requirePassed(validation);

        Optional<Simulation> existing = simulationRepository.findByValidationRunId(validationRunId);
        if (existing.isPresent()) {
            return new CreationResult(toResponse(existing.get()), false);
        }

        Simulation simulation = new Simulation();
        simulation.setValidationRun(validation);
        simulation.setStatus(SimulationStatus.READY);
        simulation.setInitialParameters(validation.getInputPayload().deepCopy());
        return new CreationResult(toResponse(simulationRepository.save(simulation)), true);
    }

    @Transactional(readOnly = true)
    public SimulationResponse get(UUID simulationId) {
        User owner = currentUserService.requireCurrentUser();
        Simulation simulation = simulationRepository
                .findByIdAndValidationRunSpecificationSubmissionOwner(simulationId, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Simulation not found"));
        return toResponse(simulation);
    }

    private void requirePassed(ValidationRun validation) {
        ValidationRunStatus status = validation.getStatus();
        if (status == ValidationRunStatus.PASSED) {
            if (validation.getModuleRelease() == null
                    || validation.getModuleRelease().getStatus() != ModuleReleaseStatus.APPROVED) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Validation module release is not approved; simulation creation is not allowed");
            }
            return;
        }
        String reason = switch (status) {
            case FAILED -> "Validation run failed physics comparison";
            case ERROR -> "Validation run ended with an execution error";
            case PENDING, RUNNING -> "Validation run has not completed";
            case PASSED -> throw new IllegalStateException("Passed validation should have returned");
        };
        throw new ApiException(HttpStatus.CONFLICT, reason + "; simulation creation is not allowed");
    }

    private SimulationResponse toResponse(Simulation simulation) {
        ValidationRun validation = simulation.getValidationRun();
        return new SimulationResponse(
                simulation.getId(),
                validation.getId(),
                validation.getSpecification().getId(),
                validation.getArchetype(),
                simulation.getStatus(),
                simulation.getInitialParameters(),
                simulation.getSceneDefinition(),
                simulation.getCreatedAt(),
                simulation.getUpdatedAt());
    }

    public record CreationResult(SimulationResponse simulation, boolean created) {
    }
}
