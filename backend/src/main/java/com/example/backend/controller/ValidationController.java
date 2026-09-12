package com.example.backend.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.validation.ValidationRunResponse;
import com.example.backend.service.PhysicsValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
public class ValidationController {

    private final PhysicsValidationService physicsValidationService;

    @Operation(summary = "Get an owned physics validation run")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation run found"),
            @ApiResponse(responseCode = "404", description = "Validation run not found or not owned")
    })
    @GetMapping("/{id}")
    public ValidationRunResponse get(@PathVariable UUID id) {
        return physicsValidationService.get(id);
    }
}
