package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.CreateModuleReleaseRequest;
import com.example.backend.dto.CreateSchemaVersionRequest;
import com.example.backend.dto.CreateSolverVersionRequest;
import com.example.backend.dto.SchemaVersionResponse;
import com.example.backend.dto.SolverVersionResponse;
import com.example.backend.dto.TopicModuleReleaseResponse;
import com.example.backend.dto.UpdateLifecycleRequest;
import com.example.backend.service.ReviewerSchemaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Reviewer - Schema & Solver Management", description = "Định nghĩa và quản lý vòng đời Schema, Solver Contracts, Module Releases (Dành cho REVIEWER & ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/reviewer")
@PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
@AllArgsConstructor
public class ReviewerSchemaController {

    private final ReviewerSchemaService schemaService;

    // ------------------------------------------------------------------ //
    // 1. Schema Versions
    // ------------------------------------------------------------------ //

    @Operation(summary = "Danh sách phiên bản Schema", description = "Lấy danh sách các schema vật lý, hỗ trợ lọc theo topic hoặc lifecycle_status.")
    @GetMapping("/schemas")
    public ResponseEntity<List<SchemaVersionResponse>> getSchemaVersions(
            @Parameter(description = "Lọc theo chủ đề: kinematics, dynamics, circuits")
            @RequestParam(required = false) String topic,
            @Parameter(description = "Lọc theo vòng đời: DRAFT, APPROVED, RETIRED")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(schemaService.getSchemaVersions(topic, status));
    }

    @Operation(summary = "Chi tiết Schema Version theo ID")
    @GetMapping("/schemas/{id}")
    public ResponseEntity<SchemaVersionResponse> getSchemaVersionById(@PathVariable UUID id) {
        return ResponseEntity.ok(schemaService.getSchemaVersionById(id));
    }

    @Operation(summary = "Tạo mới bản nháp Schema (DRAFT)")
    @PostMapping("/schemas")
    public ResponseEntity<SchemaVersionResponse> createSchemaVersion(@Valid @RequestBody CreateSchemaVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaService.createSchemaVersion(request));
    }

    @Operation(summary = "Cập nhật vòng đời Schema (DRAFT -> APPROVED -> RETIRED)", description = "Phê duyệt hoặc dừng phát hành phiên bản schema.")
    @PatchMapping("/schemas/{id}/lifecycle")
    public ResponseEntity<SchemaVersionResponse> updateSchemaLifecycle(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLifecycleRequest request) {
        return ResponseEntity.ok(schemaService.updateSchemaLifecycle(id, request.getLifecycleStatus()));
    }

    // ------------------------------------------------------------------ //
    // 2. Solver Versions
    // ------------------------------------------------------------------ //

    @Operation(summary = "Danh sách phiên bản Solver Contracts", description = "Lấy danh sách các hợp đồng solver theo schema_id hoặc lifecycle_status.")
    @GetMapping("/solvers")
    public ResponseEntity<List<SolverVersionResponse>> getSolverVersions(
            @Parameter(description = "Lọc theo schemaId (ví dụ: KINEMATICS_1D)")
            @RequestParam(required = false) String schemaId,
            @Parameter(description = "Lọc theo vòng đời: DRAFT, APPROVED, RETIRED")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(schemaService.getSolverVersions(schemaId, status));
    }

    @Operation(summary = "Tạo mới bản nháp Solver Version (DRAFT)")
    @PostMapping("/solvers")
    public ResponseEntity<SolverVersionResponse> createSolverVersion(@Valid @RequestBody CreateSolverVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaService.createSolverVersion(request));
    }

    @Operation(summary = "Cập nhật vòng đời Solver (DRAFT -> APPROVED -> RETIRED)")
    @PatchMapping("/solvers/{id}/lifecycle")
    public ResponseEntity<SolverVersionResponse> updateSolverLifecycle(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLifecycleRequest request) {
        return ResponseEntity.ok(schemaService.updateSolverLifecycle(id, request.getLifecycleStatus()));
    }

    // ------------------------------------------------------------------ //
    // 3. Topic Module Releases
    // ------------------------------------------------------------------ //

    @Operation(summary = "Danh sách phát hành Topic Module Releases", description = "Xem phiên bản phát hành tích hợp giữa Topic, Module và Schema.")
    @GetMapping("/releases")
    public ResponseEntity<List<TopicModuleReleaseResponse>> getReleases(
            @Parameter(description = "Lọc theo chủ đề: kinematics, dynamics, circuits")
            @RequestParam(required = false) String topic) {
        return ResponseEntity.ok(schemaService.getTopicModuleReleases(topic));
    }

    @Operation(summary = "Tạo mới bản nháp Release Module")
    @PostMapping("/releases")
    public ResponseEntity<TopicModuleReleaseResponse> createRelease(@Valid @RequestBody CreateModuleReleaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaService.createModuleRelease(request));
    }

    @Operation(summary = "Cập nhật vòng đời Release Module (DRAFT -> APPROVED -> RETIRED)")
    @PatchMapping("/releases/{id}/lifecycle")
    public ResponseEntity<TopicModuleReleaseResponse> updateReleaseLifecycle(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLifecycleRequest request) {
        return ResponseEntity.ok(schemaService.updateReleaseLifecycle(id, request.getLifecycleStatus()));
    }
}
