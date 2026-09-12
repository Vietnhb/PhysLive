package com.example.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.validation.ValidationRunResponse;
import com.example.backend.service.PhysicsValidationService;
import com.example.backend.dto.simulation.SimulationResponse;
import com.example.backend.service.PhysicsSimulationService;
import com.example.backend.service.PhysicsSimulationService.CreationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
public class ValidationController {

    private final PhysicsValidationService physicsValidationService;
    private final PhysicsSimulationService physicsSimulationService;

    @Operation(summary = "Get an owned physics validation run")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation run found"),
            @ApiResponse(responseCode = "404", description = "Validation run not found or not owned")
    })
    @GetMapping("/{id}")
    public ValidationRunResponse get(@PathVariable UUID id) {
        return physicsValidationService.get(id);
    }

    @Operation(summary = "Create a simulation from a passed, owned validation run")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simulation created"),
            @ApiResponse(responseCode = "200", description = "Existing simulation returned for an idempotent retry"),
            @ApiResponse(responseCode = "404", description = "Validation run not found or not owned"),
            @ApiResponse(responseCode = "409", description = "Validation run has not passed")
    })
    @PostMapping("/{id}/simulation")
    public ResponseEntity<SimulationResponse> createSimulation(@PathVariable UUID id) {
        CreationResult result = physicsSimulationService.createFromValidation(id);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.simulation());
    }
}
