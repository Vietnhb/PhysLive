package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.AssignmentResponse;
import com.example.backend.dto.AssignmentSubmissionResponse;
import com.example.backend.dto.CreateAssignmentRequest;
import com.example.backend.dto.SubmitAssignmentRequest;
import com.example.backend.dto.UpdateAssignmentRequest;
import com.example.backend.service.AssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Assignments - Class Exploration", description = "Quản lý bài tập mô phỏng, giao bài cho học sinh và nộp bài dự đoán khoa học")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/assignments")
@PreAuthorize("isAuthenticated()")
@AllArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @Operation(summary = "Danh sách bài tập", description = "Giáo viên xem các bài tập do mình tạo; Học sinh xem danh sách bài tập được giao.")
    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAssignments(Authentication authentication) {
        return ResponseEntity.ok(assignmentService.getAssignments(authentication.getName()));
    }

    @Operation(summary = "Chi tiết một bài tập theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id, authentication.getName()));
    }

    @Operation(summary = "Tạo bài tập mô phỏng mới", description = "Giáo viên tạo bài tập từ đặc tả mô phỏng và phân công danh sách học sinh.")
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.createAssignment(request, authentication.getName()));
    }

    @Operation(summary = "Cập nhật nội dung bài tập", description = "Giáo viên cập nhật thông tin, hạn nộp hoặc danh sách học sinh được giao.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignmentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request, authentication.getName()));
    }

    @Operation(summary = "Đóng / Mở bài tập", description = "Giáo viên chuyển trạng thái giữa ACTIVE và CLOSED.")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> updateStatus(
            @PathVariable UUID id,
            @Parameter(description = "Trạng thái mới: ACTIVE hoặc CLOSED", example = "CLOSED")
            @RequestParam String status,
            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.updateStatus(id, status, authentication.getName()));
    }

    @Operation(summary = "Xem tất cả bài nộp của một bài tập", description = "Dành cho Giáo viên theo dõi các dự đoán và kết quả làm bài của học sinh.")
    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getSubmissionsForAssignment(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.getSubmissionsForAssignment(id, authentication.getName()));
    }

    @Operation(summary = "Học sinh nộp dự đoán / giả thuyết khoa học cho bài tập", description = "Học sinh nộp câu trả lời dự đoán trước khi xem và chạy mô phỏng tương tác.")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<AssignmentSubmissionResponse> submitAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitAssignmentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.submitAssignment(id, request, authentication.getName()));
    }
}
