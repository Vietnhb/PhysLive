package com.example.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.health.SystemHealthResponse;
import com.example.backend.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller cung cấp endpoint kiểm tra sức khỏe hệ thống PhysLive.
 * Phục vụ cho Liveness/Readiness probe (K8s, Docker, Cloud hosting) và Swagger UI.
 */
@Tag(name = "System Health", description = "Kiểm tra tình trạng hoạt động của hệ thống, cơ sở dữ liệu, bộ nhớ và Physics Engine")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @Operation(
        summary = "Kiểm tra sức khỏe hệ thống toàn diện",
        description = "Trả về trạng thái kết nối cơ sở dữ liệu (PostgreSQL/H2), độ trễ phản hồi, dung lượng ổ đĩa, bộ nhớ JVM và danh mục mô hình vật lý đang hoạt động."
    )
    @GetMapping
    public ResponseEntity<SystemHealthResponse> getHealth() {
        SystemHealthResponse health = healthService.checkHealth();
        if ("DOWN".equalsIgnoreCase(health.getStatus())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
        return ResponseEntity.ok(health);
    }
}
