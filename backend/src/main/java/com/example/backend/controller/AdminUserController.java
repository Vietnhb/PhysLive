package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.example.backend.dto.CreateUserRequest;
import com.example.backend.dto.UpdateAccountStateRequest;
import com.example.backend.dto.UpdateUserRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.service.UserService;

import lombok.AllArgsConstructor;

/**
 * Administrative controller for managing user accounts, permissions, and lifecycle states.
 * Entire controller is protected by {@code @PreAuthorize("hasRole('ADMIN')")}.
 * Reusable error responses (400, 401, 403, 404) are applied globally via {@link StandardApiResponses}.
 */
@Tag(name = "Admin - User Management", description = "Quản trị người dùng, phân quyền RBAC, tìm kiếm và trạng thái (Dành riêng cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminUserController {

    private final UserService userService;

    // ------------------------------------------------------------------ //
    // 1. GET /api/admin/users – List / Search users
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Danh sách và tìm kiếm người dùng",
        description = "ADMIN xem danh sách hoặc tìm kiếm linh hoạt theo từ khoá email (không bắt buộc nhập đúng toàn bộ cú pháp email)."
    )
    @ApiResponse(responseCode = "200", description = "Thành công",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))))
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(
            @Parameter(description = "Từ khoá email cần tìm kiếm (tùy chọn, vd: 'gmail', 'viet', 'teacher@')")
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(userService.getUsers(email));
    }

    // ------------------------------------------------------------------ //
    // 2. POST /api/admin/users – Create User
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Tạo mới người dùng",
        description = "ADMIN tạo tài khoản mới. Email bắt buộc đúng định dạng email (chứa '@' và domain hợp lệ). Mật khẩu được mã hoá BCrypt."
    )
    @ApiResponse(responseCode = "201", description = "Tạo thành công",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // ------------------------------------------------------------------ //
    // 3. GET /api/admin/users/{id} – User details
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Lấy chi tiết người dùng theo ID",
        description = "ADMIN xem thông tin chi tiết của bất kỳ tài khoản người dùng nào."
    )
    @ApiResponse(responseCode = "200", description = "Thành công",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID của người dùng cần xem")
            @PathVariable Integer id,
            Authentication authentication) {
        return ResponseEntity.ok(userService.getUserById(id, authentication.getName(), true));
    }

    // ------------------------------------------------------------------ //
    // 4. PUT /api/admin/users/{id} – Update User
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Cập nhật thông tin người dùng",
        description = "ADMIN cập nhật thông tin bất kỳ người dùng nào, bao gồm đổi Role, đổi trường học, avatar, mật khẩu, và trạng thái enabled."
    )
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID của người dùng cần cập nhật")
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateUser(id, request, authentication.getName(), true));
    }

    // ------------------------------------------------------------------ //
    // 5. DELETE /api/admin/users/{id} – Delete User
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Xoá người dùng",
        description = "ADMIN xoá tài khoản. Có cơ chế bảo vệ: ADMIN không thể tự xoá chính mình. " +
                      "Nếu người dùng đã có dữ liệu liên kết (simulations, library_items), hệ thống sẽ tự động chuyển sang chế độ vô hiệu hoá an toàn."
    )
    @ApiResponse(responseCode = "200", description = "Xoá thành công")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "ID của người dùng cần xoá")
            @PathVariable Integer id,
            Authentication authentication) {
        userService.deleteUser(id, authentication.getName());
        return ResponseEntity.ok("Xoá người dùng thành công");
    }

    // ------------------------------------------------------------------ //
    // 6. PATCH /api/admin/users/{id}/state – Toggle account state
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Bật/tắt trạng thái tài khoản",
        description = "ADMIN có thể kích hoạt hoặc vô hiệu hoá một tài khoản. Gửi `{ \"enabled\": false }` để vô hiệu hoá, `{ \"enabled\": true }` để kích hoạt lại."
    )
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @PatchMapping("/{id}/state")
    public ResponseEntity<UserResponse> updateAccountState(
            @Parameter(description = "ID của người dùng cần thay đổi trạng thái")
            @PathVariable Integer id,
            @RequestBody UpdateAccountStateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateAccountState(id, request.isEnabled(), authentication.getName()));
    }
}
