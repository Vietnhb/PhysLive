package com.example.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.problem.ResolveAmbiguityRequest;
import com.example.backend.dto.problem.SpecificationResponse;
import com.example.backend.dto.problem.ValidationReadinessResponse;
import com.example.backend.dto.validation.ValidationRunResponse;
import com.example.backend.service.PhysicsValidationService;
import com.example.backend.service.SpecificationService;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/specifications")
@RequiredArgsConstructor
public class SpecificationController {

    private final SpecificationService specificationService;
    private final PhysicsValidationService physicsValidationService;

    @GetMapping("/{id}")
    public SpecificationResponse get(@PathVariable UUID id) {
        return specificationService.get(id);
    }

    @PostMapping("/{specificationId}/ambiguities/{ambiguityId}/confirm")
    public SpecificationResponse confirmAmbiguity(
            @PathVariable UUID specificationId,
            @PathVariable UUID ambiguityId,
            @RequestBody ResolveAmbiguityRequest request) {
        return specificationService.resolve(specificationId, ambiguityId, request);
    }

    @GetMapping("/{id}/validation-readiness")
    public ValidationReadinessResponse readiness(@PathVariable UUID id) {
        return specificationService.readiness(id);
    }

    @Operation(summary = "Run numerical/reference validation for an owned specification")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Validation run created"),
            @ApiResponse(responseCode = "404", description = "Specification not found or not owned"),
            @ApiResponse(responseCode = "409", description = "Specification is not validation-ready")
    })
    @PostMapping("/{id}/validations")
    public ResponseEntity<ValidationRunResponse> validate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(physicsValidationService.validate(id));
    }
}
