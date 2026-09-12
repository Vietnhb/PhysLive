package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request payload for authenticated users to update their own personal profile.
 * Does not permit modifying roles, permissions, or account active/enabled state.
 */
@Data
public class UpdateProfileRequest {

    @Schema(description = "Họ và tên mới (tùy chọn)", example = "Nguyễn Văn B")
    private String fullName;

    @Schema(description = "Mã trường / tổ chức (tùy chọn)", example = "THPT_CHUYEN_LEQUYDON")
    private String institutionId;

    @Schema(description = "URL ảnh đại diện mới (tùy chọn)", example = "https://example.com/my-avatar.jpg")
    private String avatarUrl;

    @Schema(description = "Mật khẩu mới (tùy chọn, để trống nếu không muốn đổi)", example = "NewPass@456")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String password;
}
