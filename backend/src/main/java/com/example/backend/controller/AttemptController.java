package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.AssignmentSubmissionResponse;
import com.example.backend.dto.CreateSimulationRunRequest;
import com.example.backend.dto.SimulationRunResponse;
import com.example.backend.service.AssignmentService;
import com.example.backend.service.SimulationAttemptService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Attempts & Simulation Runs", description = "Quản lý kết quả chạy mô phỏng tương tác và lịch sử nộp bài thử nghiệm của học sinh")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/attempts")
@PreAuthorize("isAuthenticated()")
@AllArgsConstructor
public class AttemptController {

    private final SimulationAttemptService simulationAttemptService;
    private final AssignmentService assignmentService;

    @Operation(summary = "Ghi lại phiên chạy/thử nghiệm mô phỏng", description = "Lưu kết quả tính toán toạ độ, thời lượng và trạng thái kiểm định nghiệm của lần chạy mô phỏng.")
    @PostMapping("/simulations/run")
    public ResponseEntity<SimulationRunResponse> recordSimulationRun(
            @Valid @RequestBody CreateSimulationRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationAttemptService.recordSimulationRun(request));
    }

    @Operation(summary = "Lịch sử các lần chạy mô phỏng", description = "Xem lại lịch sử kết quả tính toán của một mô phỏng cụ thể.")
    @GetMapping("/simulations/{id}/runs")
    public ResponseEntity<List<SimulationRunResponse>> getSimulationRuns(
            @PathVariable UUID id) {
        return ResponseEntity.ok(simulationAttemptService.getSimulationRuns(id));
    }

    @Operation(summary = "Lịch sử các bài tập đã nộp của tôi", description = "Dành cho Học sinh xem lại các giả thuyết và câu trả lời dự đoán đã nộp cho các bài tập.")
    @GetMapping("/assignments/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getMySubmissions(
            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.getMySubmissions(authentication.getName()));
    }
}
