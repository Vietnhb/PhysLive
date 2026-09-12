package com.example.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.ExtractionRunResponse;
import com.example.backend.dto.PipelineTelemetryResponse;
import com.example.backend.service.TelemetryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "Admin - Pipeline Telemetry", description = "Giám sát sức khỏe hệ thống, tỷ lệ bóc tách bài toán và log lỗi pipeline (Dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/admin/telemetry")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminTelemetryController {

    private final TelemetryService telemetryService;

    @Operation(
        summary = "Tổng quan Telemetry Pipeline bóc tách bài toán",
        description = "Trả về tổng số lượt chạy, số lượt thành công, số lượt lỗi, tỷ lệ thành công %, phân loại theo Provider (OPENROUTER, RULE_BASED) và top 10 lỗi gần nhất."
    )
    @GetMapping("/overview")
    public ResponseEntity<PipelineTelemetryResponse> getOverview() {
        return ResponseEntity.ok(telemetryService.getPipelineTelemetry());
    }

    @Operation(
        summary = "Danh sách lịch sử các lượt chạy bóc tách (Extraction Runs)",
        description = "Lấy danh sách các lượt chạy bóc tách mới nhất, có thể lọc theo trạng thái (RUNNING, SUCCEEDED, FAILED)."
    )
    @GetMapping("/extraction-runs")
    public ResponseEntity<List<ExtractionRunResponse>> getExtractionRuns(
            @Parameter(description = "Lọc theo trạng thái: RUNNING, SUCCEEDED, FAILED (tùy chọn)")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(telemetryService.getRecentRuns(status));
    }
}
