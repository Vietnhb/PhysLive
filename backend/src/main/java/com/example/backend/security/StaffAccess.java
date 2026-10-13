package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Bridges teacher endpoints until the Staff workspace is migrated. */
@Component
@RequiredArgsConstructor
public class StaffAccess {
    private final CurrentUserService currentUser;

    public static boolean canTeach(User user) {
        if (user == null || user.getRole() == null) return false;
        String role = user.getRole().getName();
        return "TEACHER".equals(role) || "ADMIN".equals(role)
                || ("STAFF".equals(role) && user.hasPermission("TEACH"));
    }

    public User requireTeaching() {
        User user = currentUser.requireCurrentUser();
        if (!canTeach(user)) throw new ApiException(HttpStatus.FORBIDDEN, "Teaching permission is required");
        return user;
    }
}
