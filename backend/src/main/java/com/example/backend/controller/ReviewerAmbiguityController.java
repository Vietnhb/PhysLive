package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.AmbiguityCaseResponse;
import com.example.backend.dto.ReviewDecisionRequest;
import com.example.backend.dto.ReviewerDecisionResponse;
import com.example.backend.service.ReviewerAmbiguityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Reviewer - Ambiguity Adjudication", description = "Kiểm tra và trọng tài phân xử các ca mơ hồ được chuyển giao từ pipeline (Dành cho REVIEWER & ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/reviewer/ambiguities")
@PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
@AllArgsConstructor
public class ReviewerAmbiguityController {

    private final ReviewerAmbiguityService ambiguityService;

    @Operation(
        summary = "Danh sách các ca mơ hồ cần phân xử",
        description = "Lấy danh sách các ca mơ hồ (Ambiguity Cases) do pipeline bóc tách phát hiện, lọc theo trạng thái: OPEN, RESOLVED, REJECTED."
    )
    @GetMapping
    public ResponseEntity<List<AmbiguityCaseResponse>> getAmbiguityCases(
            @Parameter(description = "Lọc theo trạng thái: OPEN, RESOLVED, REJECTED (tùy chọn)")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ambiguityService.getAmbiguityCases(status));
    }

    @Operation(summary = "Chi tiết ca mơ hồ theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<AmbiguityCaseResponse> getAmbiguityCaseById(@PathVariable UUID id) {
        return ResponseEntity.ok(ambiguityService.getAmbiguityCaseById(id));
    }

    @Operation(
        summary = "Trọng tài phân xử ca mơ hồ (Adjudicate)",
        description = "Reviewer đưa ra câu trả lời giải quyết vấn đề mơ hồ (RESOLVED) hoặc từ chối giải quyết (REJECTED), hệ thống tự động ghi nhận actor và thời gian quyết định."
    )
    @PostMapping("/{id}/adjudicate")
    public ResponseEntity<ReviewerDecisionResponse> adjudicateCase(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewDecisionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ambiguityService.adjudicate(id, request, authentication.getName()));
    }

    @Operation(summary = "Lịch sử các quyết định phân xử trên ca mơ hồ này")
    @GetMapping("/{id}/decisions")
    public ResponseEntity<List<ReviewerDecisionResponse>> getDecisionsByCase(@PathVariable UUID id) {
        return ResponseEntity.ok(ambiguityService.getDecisionsByCase(id));
    }
}
