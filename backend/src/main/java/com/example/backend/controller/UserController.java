package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.UpdateProfileRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.service.UserService;

import lombok.AllArgsConstructor;

/**
 * Self-service user profile controller.
 * Allows authenticated users to view and update their own personal profile.
 * Standard error responses (400, 401, 403, 404) are applied via {@link StandardApiResponses}.
 */
@Tag(name = "User Profile", description = "Quản lý thông tin tài khoản cá nhân (Dành cho người dùng tự thao tác)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/user")
@PreAuthorize("isAuthenticated()")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    // ------------------------------------------------------------------ //
    // 1. GET /api/user/me – Profile of current user
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Lấy thông tin tài khoản hiện tại",
        description = "Trả về profile cá nhân của user đang đăng nhập dựa trên JWT token."
    )
    @ApiResponse(responseCode = "200", description = "Thành công",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    // ------------------------------------------------------------------ //
    // 2. PUT /api/user/me – Update current user's profile
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Cập nhật hồ sơ cá nhân",
        description = "Người dùng tự cập nhật thông tin cá nhân của mình (họ tên, trường học, avatar, mật khẩu mới)."
    )
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateMyProfile(authentication.getName(), request));
    }
}
