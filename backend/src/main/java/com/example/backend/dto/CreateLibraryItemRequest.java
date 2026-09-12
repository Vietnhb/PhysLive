package com.example.backend.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateLibraryItemRequest {

    @Schema(description = "Tiêu đề mô phỏng lưu trong thư viện", example = "Chuyển động ném ngang với sức cản không khí")
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @Schema(description = "ID của đặc tả mô phỏng (Specification)", example = "11111111-1111-1111-1111-111111111111")
    @NotNull(message = "specificationId không được để trống")
    private UUID specificationId;

    @Schema(description = "Chế độ hiển thị: PERSONAL (Cá nhân) hoặc SHARED (Chia sẻ toàn trường)", example = "SHARED")
    @Pattern(regexp = "PERSONAL|SHARED", message = "Visibility phải là PERSONAL hoặc SHARED")
    private String visibility = "SHARED";
}
