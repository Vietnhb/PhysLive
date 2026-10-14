package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LibraryItemResponse {

    @Schema(description = "ID mục thư viện")
    private UUID id;

    @Schema(description = "Tiêu đề mô phỏng")
    private String title;

    @Schema(description = "Quyền hiển thị: PERSONAL hoặc SHARED")
    private String visibility;

    private String moderationStatus;
    private String moderationComment;
    private java.time.Instant moderatedAt;
    private Integer moderatedBy;
    private String sharedInstitutionId;
    private UUID schoolId;
    private String schoolName;

    @Schema(description = "Trạng thái hiển thị")
    private boolean active;

    @Schema(description = "ID người sở hữu")
    private Integer ownerId;

    @Schema(description = "Tên người sở hữu")
    private String ownerName;

    @Schema(description = "Email người sở hữu")
    private String ownerEmail;

    @Schema(description = "ID đặc tả mô phỏng")
    private UUID specificationId;

    @Schema(description = "Chủ đề vật lý của đặc tả (vd: kinematics, dynamics, circuits)")
    private String topic;

    @Schema(description = "Chi tiết các đối tượng và tham số của đặc tả mô phỏng (JSONB)")
    private Object specificationDetails;

    @Schema(description = "Thời điểm tạo")
    private OffsetDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật")
    private OffsetDateTime updatedAt;
}
