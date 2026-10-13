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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.CreateLibraryItemRequest;
import com.example.backend.dto.LibraryItemResponse;
import com.example.backend.dto.UpdateLibraryItemRequest;
import com.example.backend.service.LibraryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Library - Simulation Repository", description = "Quản lý kho thư viện mô phỏng vật lý cá nhân và dùng chung (Dành cho Giáo viên & Học sinh)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/library")
@PreAuthorize("isAuthenticated()")
@AllArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @Operation(summary = "Tìm kiếm và xem danh sách mô phỏng trong thư viện", description = "Lấy danh sách các mô phỏng được chia sẻ (SHARED) hoặc mô phỏng cá nhân (PERSONAL) có hỗ trợ tìm kiếm theo từ khoá tiêu đề.")
    @GetMapping
    public ResponseEntity<List<LibraryItemResponse>> getLibraryItems(
            @Parameter(description = "Chế độ hiển thị: SHARED hoặc PERSONAL (để trống để lấy cả hai)")
            @RequestParam(required = false) String visibility,
            @Parameter(description = "Từ khoá tìm kiếm theo tiêu đề mô phỏng")
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return ResponseEntity.ok(libraryService.getLibraryItems(authentication.getName(), visibility, search));
    }

    @Operation(summary = "Xem chi tiết một mô phỏng trong thư viện theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<LibraryItemResponse> getLibraryItemById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(libraryService.getLibraryItemById(id, authentication.getName()));
    }

    @Operation(summary = "Lưu một mô phỏng mới vào thư viện", description = "Dành cho Giáo viên hoặc Admin lưu một đặc tả mô phỏng (Specification) vào kho tài nguyên.")
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<LibraryItemResponse> createLibraryItem(
            @Valid @RequestBody CreateLibraryItemRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.createLibraryItem(request, authentication.getName()));
    }

    @Operation(summary = "Cập nhật thông tin mô phỏng trong thư viện", description = "Chủ sở hữu hoặc Admin có thể đổi tiêu đề, quyền riêng tư (SHARED/PERSONAL) hoặc ẩn mô phỏng.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<LibraryItemResponse> updateLibraryItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLibraryItemRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(libraryService.updateLibraryItem(id, request, authentication.getName()));
    }

    @Operation(summary = "Xoá mô phỏng khỏi thư viện (Soft Delete)", description = "Ẩn mô phỏng khỏi thư viện hiển thị.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<Void> deleteLibraryItem(
            @PathVariable UUID id,
            Authentication authentication) {
        libraryService.deleteLibraryItem(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Nhân bản (Clone) mô phỏng vào kho cá nhân", description = "Giáo viên sao chép một mô phỏng dùng chung vào kho cá nhân của mình để tuỳ biến.")
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<LibraryItemResponse> cloneLibraryItem(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.cloneLibraryItem(id, authentication.getName()));
    }
}
