package com.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import com.example.backend.dto.CreateUserRequest;
import com.example.backend.dto.ErrorResponse;
import com.example.backend.dto.UpdateAccountStateRequest;
import com.example.backend.dto.UpdateUserRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.service.UserService;

import lombok.AllArgsConstructor;

/**
 * Controller managing users, profile operations, and account state.
 *
 * <p>Access control:
 * <ul>
 *   <li>ADMIN (role_id = 1) has global CRUD access to all user accounts.</li>
 *   <li>Standard users can view and edit only their own profile (Ownership check).</li>
 * </ul>
 */
@Tag(name = "User", description = "Quản lý người dùng, tìm kiếm, phân quyền RBAC và trạng thái tài khoản")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    // ------------------------------------------------------------------ //
    // 1. GET /api/user/me – Profile of current user
    // ------------------------------------------------------------------ //

    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Trả về thông tin profile của user từ JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    // ------------------------------------------------------------------ //
    // 2. POST /api/user – Create User (ADMIN only)
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Tạo mới người dùng (ADMIN)",
        description = "ADMIN tạo tài khoản mới. Email bắt buộc đúng định dạng email (chứa '@' và domain hợp lệ). Mật khẩu được mã hoá BCrypt."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tạo thành công",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc email đã tồn tại",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Chỉ ADMIN mới có quyền tạo",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // ------------------------------------------------------------------ //
    // 3. GET /api/user – List / Search users (ADMIN only)
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Danh sách và tìm kiếm người dùng (ADMIN)",
        description = "ADMIN xem danh sách hoặc tìm kiếm theo email (không bắt buộc nhập đúng toàn bộ cú pháp email, có thể nhập từ khoá/chuỗi con)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Chỉ ADMIN mới có quyền xem danh sách",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @Parameter(description = "Từ khoá email cần tìm kiếm (tùy chọn, vd: 'gmail', 'viet', 'teacher@')")
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(userService.getUsers(email));
    }

    // ------------------------------------------------------------------ //
    // 4. GET /api/user/{id} – User details (ADMIN or Owner)
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Lấy chi tiết người dùng theo ID",
        description = "ADMIN có thể xem bất kỳ tài khoản nào. Người dùng thông thường chỉ có thể xem thông tin của chính mình (Ownership check)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền xem thông tin của người dùng khác",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID của người dùng cần xem")
            @PathVariable Integer id,
            Authentication authentication) {
        boolean isAdmin = checkIsAdmin(authentication);
        return ResponseEntity.ok(userService.getUserById(id, authentication.getName(), isAdmin));
    }

    // ------------------------------------------------------------------ //
    // 5. PUT /api/user/{id} – Update User (ADMIN or Owner)
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Cập nhật thông tin người dùng",
        description = "Người dùng thông thường chỉ có thể cập nhật thông tin của chính mình (fullName, institutionId, password). " +
                      "Chỉ ADMIN mới có quyền thay đổi roleId và trạng thái enabled."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc ADMIN tự vô hiệu hoá chính mình",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Không có quyền sửa đổi người dùng khác hoặc nâng quyền",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID của người dùng cần cập nhật")
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        boolean isAdmin = checkIsAdmin(authentication);
        return ResponseEntity.ok(userService.updateUser(id, request, authentication.getName(), isAdmin));
    }

    // ------------------------------------------------------------------ //
    // 6. DELETE /api/user/{id} – Delete User (ADMIN only)
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Xoá người dùng (ADMIN)",
        description = "ADMIN xoá tài khoản. Có cơ chế bảo vệ: ADMIN không thể tự xoá chính mình. " +
                      "Nếu người dùng đã có dữ liệu ràng buộc (simulations, library_items), hệ thống sẽ tự động chuyển sang chế độ vô hiệu hoá an toàn."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Xoá thành công"),
        @ApiResponse(responseCode = "400", description = "ADMIN không thể tự xoá tài khoản của chính mình",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Chỉ ADMIN mới có quyền xoá",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "ID của người dùng cần xoá")
            @PathVariable Integer id,
            Authentication authentication) {
        userService.deleteUser(id, authentication.getName());
        return ResponseEntity.ok("Xoá người dùng thành công");
    }

    // ------------------------------------------------------------------ //
    // 7. PATCH /api/user/{id}/state – Toggle account enabled state (ADMIN only)
    // ------------------------------------------------------------------ //

    @Operation(
        summary = "Bật/tắt trạng thái tài khoản (ADMIN)",
        description = "ADMIN có thể kích hoạt hoặc vô hiệu hoá một tài khoản. Gửi `{ \"enabled\": false }` để vô hiệu hoá, `{ \"enabled\": true }` để kích hoạt lại."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "ADMIN không thể tự vô hiệu hoá tài khoản của chính mình",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Chỉ ADMIN mới có quyền thực hiện",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/state")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateAccountState(
            @Parameter(description = "ID của người dùng cần thay đổi trạng thái")
            @PathVariable Integer id,
            @RequestBody UpdateAccountStateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateAccountState(id, request.isEnabled(), authentication.getName()));
    }

    // ------------------------------------------------------------------ //
    // Helper
    // ------------------------------------------------------------------ //

    private boolean checkIsAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
