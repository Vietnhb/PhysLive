package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload to update profile or credentials.
 * Regular users can update their own fullName, institutionId, password.
 * Only ADMIN (roleId = 1) can update roleId and enabled flag.
 */
@Data
public class UpdateUserRequest {

    @Schema(description = "Họ và tên mới (tùy chọn)", example = "Nguyễn Văn B")
    private String fullName;

    @Schema(description = "Mật khẩu mới (tùy chọn, để trống nếu không muốn đổi)", example = "NewPass@456")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String password;

    @Schema(description = "Mã trường / tổ chức (tùy chọn)", example = "THPT_CHUYEN_LEQUYDON")
    private String institutionId;

    @Schema(description = "URL ảnh đại diện mới (tùy chọn)", example = "https://example.com/new-avatar.jpg")
    private String avatarUrl;

    @Schema(description = "Role ID mới: 1=ADMIN, 2=MANAGER, 3=REVIEWER, 4=SCHOOL, 5=STAFF, 6=STUDENT", example = "5")
    private Integer roleId;

    @Schema(description = "Trạng thái kích hoạt tài khoản (chỉ ADMIN có quyền sửa)", example = "true")
    private Boolean enabled;
}
