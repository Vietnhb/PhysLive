package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload used by ADMIN to provision new users.
 */
@Data
public class CreateUserRequest {

    @Schema(description = "Email đăng nhập (bắt buộc đúng định dạng email có '@')", example = "teacher.nguyen@physlive.edu.vn")
    @NotBlank(message = "Email không được để trống")
    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email không đúng định dạng (phải chứa ký tự '@' và tên miền hợp lệ)")
    private String email;

    @Schema(description = "Mật khẩu ban đầu (ít nhất 6 ký tự)", example = "Password@123")
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn A")
    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @Schema(description = "Role ID: 1 = ADMIN, 2 = TEACHER, 3 = REVIEWER, 4 = STUDENT, 5 = GUEST (mặc định 5 nếu không truyền)", example = "2")
    private Integer roleId;

    @Schema(description = "Mã trường / tổ chức (tùy chọn)", example = "THPT_CHUYEN_HANOI")
    private String institutionId;

    @Schema(description = "URL ảnh đại diện (tùy chọn)", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "Trạng thái kích hoạt tài khoản (mặc định true)", example = "true")
    private Boolean enabled = true;
}
