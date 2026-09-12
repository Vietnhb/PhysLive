package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateLibraryItemRequest {

    @Schema(description = "Tiêu đề mới cho mô phỏng", example = "Chuyển động ném ngang (Bản chỉnh sửa)")
    private String title;

    @Schema(description = "Chế độ hiển thị: PERSONAL hoặc SHARED", example = "PERSONAL")
    @Pattern(regexp = "PERSONAL|SHARED", message = "Visibility phải là PERSONAL hoặc SHARED")
    private String visibility;

    @Schema(description = "Trạng thái kích hoạt (false để ẩn)", example = "true")
    private Boolean active;
}
