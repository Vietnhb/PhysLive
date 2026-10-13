package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.ClassSessionResponse;
import com.example.backend.dto.CreateClassSessionRequest;
import com.example.backend.service.ClassSessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Class Sessions - Live Player & Replay", description = "Ghi lại và phát lại phiên biểu diễn mô phỏng trên lớp (Dành cho Giáo viên trình chiếu & Học sinh xem lại)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("isAuthenticated()")
@AllArgsConstructor
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @Operation(summary = "Danh sách các phiên biểu diễn mô phỏng", description = "Xem danh sách các buổi học/phiên mô phỏng đã được ghi lại dòng thời gian thay đổi tham số.")
    @GetMapping
    public ResponseEntity<List<ClassSessionResponse>> getSessions(Authentication authentication) {
        return ResponseEntity.ok(classSessionService.getSessions(authentication.getName()));
    }

    @Operation(summary = "Xem chi tiết và phát lại (Replay) phiên mô phỏng", description = "Học sinh và giáo viên tải lại đặc tả ban đầu cùng toàn bộ chuỗi sự kiện slider/tham số theo thời gian thực để phát lại trên Player Canvas.")
    @GetMapping("/{id}")
    public ResponseEntity<ClassSessionResponse> getSessionById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(classSessionService.getSessionById(id, authentication.getName()));
    }

    @Operation(summary = "Ghi lại phiên biểu diễn mô phỏng trên lớp", description = "Giáo viên lưu lại buổi học gồm đặc tả bài toán và timeline các lần kéo thanh trượt/slider.")
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<ClassSessionResponse> createSession(
            @Valid @RequestBody CreateClassSessionRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classSessionService.createSession(request, authentication.getName()));
    }

    @Operation(summary = "Xoá phiên biểu diễn", description = "Giáo viên hoặc Admin xoá bản ghi buổi biểu diễn.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID id,
            Authentication authentication) {
        classSessionService.deleteSession(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
