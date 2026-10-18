package com.example.backend.system.account.dto;

import com.example.backend.system.account.model.entity.User;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Integer id;
    private String email;
    private String fullName;
    private String role;
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private java.util.UUID schoolId;
    private boolean billingRequired;
    private boolean mustChangePassword;
    private String schoolName;
    private boolean active;
    /** Permission codes granted through user_permissions, e.g. TEACH, DEPARTMENT_HEAD_PHYSICS. */
    private java.util.List<String> permissions;
    private String accountStatus;
    private String suspensionReason;
    private Instant suspendedUntil;

    public static UserResponse from(User user, boolean billingRequired) {
        String status = user.getAccountStatus() != null ? user.getAccountStatus()
                : (Boolean.TRUE.equals(user.getActive()) ? "ACTIVE" : "SUSPENDED_READ_ONLY");
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole() == null ? "UNKNOWN" : user.getRole().getName(),
                user.getDateOfBirth(), user.getAvatarUrl(),
                user.getSchool() == null ? null : user.getSchool().getId(), billingRequired,
                user.isMustChangePassword(), user.getSchool() == null ? null : user.getSchool().getName(),
                Boolean.TRUE.equals(user.getActive()), user.permissionCodes(),
                status, user.getSuspensionReason(), user.getSuspendedUntil());
    }
}
