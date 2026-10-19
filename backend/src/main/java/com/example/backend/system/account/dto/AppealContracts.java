package com.example.backend.system.account.dto;

import com.example.backend.system.account.model.entity.AccountAppeal;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class AppealContracts {
    private AppealContracts() { }
    public record CreateAppealRequest(@NotBlank String reason, @NotBlank String content) { }
    public record ResolveAppealRequest(@NotBlank String status, String resolutionNote) { }
    public record AppealResponse(UUID id, Integer userId, String userEmail, String userName,
                                  String reason, String content, String status, String resolutionNote,
                                  Instant createdAt, Instant resolvedAt) {
        public static AppealResponse from(AccountAppeal item) {
            return new AppealResponse(item.getId(), item.getUser().getId(), item.getUser().getEmail(),
                    item.getUser().getFullName(), item.getReason(), item.getContent(),
                    item.getStatus(), item.getResolutionNote(), item.getCreatedAt(), item.getResolvedAt());
        }
    }

}
