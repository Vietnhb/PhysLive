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
    private final com.example.backend.service.CurrentUserService currentUser;
    private final com.example.backend.service.school.LicenseCheckService license;

    @MessageMapping("/simulation/adjust")
    @SendToUser("/queue/simulation")
    public SimulationRunResponse adjust(@Valid ParameterAdjustmentRequest request, java.security.Principal principal) {
        if (!(principal instanceof org.springframework.security.core.Authentication authentication) || !authentication.isAuthenticated())
            throw new com.example.backend.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication is required");
        var previous = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            var actor = currentUser.requireAnyRole("STAFF", "MANAGER");
            license.requireWriteAccess(actor);
            return simulationRunService.create(request.simulationId(), request.toRunRequest());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.setContext(previous);
        }
    }
}
