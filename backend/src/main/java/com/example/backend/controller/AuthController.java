package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ErrorResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.service.AuthService;

import lombok.AllArgsConstructor;

/**
 * Authentication endpoints – all public (no JWT required).
 */
@Tag(name = "Auth", description = "Đăng nhập và đăng ký tài khoản")
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ------------------------------------------------------------------ //
    // POST /api/auth/login
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Đăng nhập",
        description = "Xác thực bằng email/password và nhận JWT Bearer token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Email hoặc mật khẩu không đúng",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Tài khoản bị vô hiệu hoá",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@jakarta.validation.Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
    }

    // ------------------------------------------------------------------ //
    // POST /api/auth/signup
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Đăng ký",
        description = "Tạo tài khoản mới với role GUEST. Password được mã hoá BCrypt trước khi lưu."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đăng ký thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc email đã tồn tại",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@jakarta.validation.Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("Đăng ký thành công");
    }
}
