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

import com.example.backend.dto.release.CreateTopicModuleReleaseRequest;
import com.example.backend.dto.release.TopicModuleReleaseResponse;
import com.example.backend.service.TopicModuleReleaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/module-releases")
@RequiredArgsConstructor
public class TopicModuleReleaseController {

    private final TopicModuleReleaseService releaseService;

    @Operation(summary = "Create an immutable draft topic module release")
    @PostMapping
    public ResponseEntity<TopicModuleReleaseResponse> create(
            @RequestBody CreateTopicModuleReleaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(releaseService.createDraft(request));
    }

    @Operation(summary = "Get a topic module release")
    @GetMapping("/{id}")
    public TopicModuleReleaseResponse get(@PathVariable UUID id) {
        return releaseService.get(id);
    }

    @Operation(summary = "Transition a draft release to approved")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Release approved"),
            @ApiResponse(responseCode = "409", description = "Transition or release configuration is invalid")
    })
    @PostMapping("/{id}/approve")
    public TopicModuleReleaseResponse approve(@PathVariable UUID id) {
        return releaseService.approve(id);
    }

    @Operation(summary = "Transition an approved release to retired")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Release retired"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "409", description = "Only approved releases can be retired")
    })
    @PostMapping("/{id}/retire")
    public TopicModuleReleaseResponse retire(@PathVariable UUID id) {
        return releaseService.retire(id);
    }
}
