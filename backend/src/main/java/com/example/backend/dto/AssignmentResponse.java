package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResponse {

    @Schema(description = "ID bài tập")
    private UUID id;

    @Schema(description = "Tiêu đề bài tập")
    private String title;

    @Schema(description = "Mô tả bài tập")
    private String description;

    @Schema(description = "Trạng thái bài tập: ACTIVE hoặc CLOSED")
    private String status;

    @Schema(description = "Thời gian giao bài")
    private OffsetDateTime assignedAt;

    @Schema(description = "Hạn nộp bài")
    private OffsetDateTime dueAt;

    @Schema(description = "Nội dung câu hỏi dự đoán (JSONB)")
    private Object questions;

    @Schema(description = "ID giáo viên giao bài")
    private Integer teacherId;

    @Schema(description = "Tên giáo viên giao bài")
    private String teacherName;

    @Schema(description = "ID đặc tả mô phỏng")
    private UUID specificationId;

    @Schema(description = "Số lượng học sinh được giao bài")
    private int assignedStudentCount;

    @Schema(description = "Danh sách ID học sinh được giao bài")
    private List<Integer> assignedStudentIds;

    @Schema(description = "Số lượng bài nộp đã nhận")
    private int submissionCount;

    @Schema(description = "Thời điểm tạo")
    private OffsetDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật")
    private OffsetDateTime updatedAt;
}
