package com.example.backend.controller;

import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.simulation.SimulationResponse;
import com.example.backend.dto.simulation.CreateSimulationRunRequest;
import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.service.PhysicsSimulationService;
import com.example.backend.service.PhysicsSimulationRunService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final PhysicsSimulationService physicsSimulationService;
    private final PhysicsSimulationRunService simulationRunService;

    @Operation(summary = "List owned simulations newest first")
    @GetMapping
    public List<SimulationResponse> history() {
        return physicsSimulationService.history();
    }

    @Operation(summary = "List the ten most recent owned simulations")
    @GetMapping("/recent")
    public List<SimulationResponse> recent() {
        return physicsSimulationService.recent();
    }

    @Operation(summary = "Get an owned simulation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation found"),
            @ApiResponse(responseCode = "404", description = "Simulation not found or not owned")
    })
    @GetMapping("/{id}")
    public SimulationResponse get(@PathVariable UUID id) {
        return physicsSimulationService.get(id);
    }

    @Operation(summary = "Re-solve an owned simulation with allowed parameter overrides")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simulation run created"),
            @ApiResponse(responseCode = "404", description = "Simulation not found or not owned"),
            @ApiResponse(responseCode = "409", description = "Simulation is not ready"),
            @ApiResponse(responseCode = "422", description = "Parameter overrides are invalid")
    })
    @PostMapping("/{id}/runs")
    public ResponseEntity<SimulationRunResponse> createRun(
            @PathVariable UUID id,
            @RequestBody(required = false) CreateSimulationRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationRunService.create(id, request));
    }
}
