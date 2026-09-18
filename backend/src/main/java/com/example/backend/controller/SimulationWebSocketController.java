package com.example.backend.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.example.backend.dto.simulation.ParameterAdjustmentRequest;
import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.service.PhysicsSimulationRunService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@Validated
@RequiredArgsConstructor
public class SimulationWebSocketController {
    private final PhysicsSimulationRunService simulationRunService;

    @MessageMapping("/simulation/adjust")
    @SendToUser("/queue/simulation")
    public SimulationRunResponse adjust(@Valid ParameterAdjustmentRequest request) {
        return simulationRunService.create(request.simulationId(), request.toRunRequest());
    }
}
