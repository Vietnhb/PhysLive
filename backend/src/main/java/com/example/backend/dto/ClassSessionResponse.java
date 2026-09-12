package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassSessionResponse {

    @Schema(description = "ID buổi học / phiên biểu diễn")
    private UUID id;

    @Schema(description = "Tiêu đề buổi học")
    private String title;

    @Schema(description = "Thời lượng biểu diễn (giây)")
    private Double durationSeconds;

    @Schema(description = "Đặc tả ban đầu (JSONB)")
    private Object initialSpecification;

    @Schema(description = "Dòng thời gian các lần thay đổi tham số (JSONB)")
    private Object parameterTimeline;

    @Schema(description = "Đường dẫn lưu trữ (tùy chọn)")
    private String storageUrl;

    @Schema(description = "ID giáo viên thực hiện")
    private Integer teacherId;

    @Schema(description = "Tên giáo viên")
    private String teacherName;

    @Schema(description = "ID đặc tả mô phỏng")
    private UUID specificationId;

    @Schema(description = "Thời điểm tạo")
    private OffsetDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật")
    private OffsetDateTime updatedAt;
}
