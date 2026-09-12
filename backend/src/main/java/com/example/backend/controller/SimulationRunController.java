package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.dto.simulation.SnapshotResponse;
import com.example.backend.service.PhysicsSimulationRunService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class SimulationRunController {

    private final PhysicsSimulationRunService simulationRunService;

    @Operation(summary = "Get an immutable owned simulation run")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation run found"),
            @ApiResponse(responseCode = "404", description = "Simulation run not found or not owned")
    })
    @GetMapping("/{id}")
    public SimulationRunResponse get(@PathVariable UUID id) {
        return simulationRunService.get(id);
    }

    @Operation(summary = "Replay an owned simulation run from persisted snapshots")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordered snapshots returned"),
            @ApiResponse(responseCode = "404", description = "Simulation run not found or not owned")
    })
    @GetMapping("/{id}/snapshots")
    public List<SnapshotResponse> snapshots(@PathVariable UUID id) {
        return simulationRunService.snapshots(id);
    }
}
