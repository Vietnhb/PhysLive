package com.example.backend.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.simulation.SimulationResponse;
import com.example.backend.service.PhysicsSimulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final PhysicsSimulationService physicsSimulationService;

    @Operation(summary = "Get an owned simulation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation found"),
            @ApiResponse(responseCode = "404", description = "Simulation not found or not owned")
    })
    @GetMapping("/{id}")
    public SimulationResponse get(@PathVariable UUID id) {
        return physicsSimulationService.get(id);
    }
}
