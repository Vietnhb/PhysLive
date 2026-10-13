package com.example.backend.service.account;

import com.example.backend.entity.Permission;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.PermissionCode;
import com.example.backend.entity.enums.RoleName;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.PermissionRepository;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Manages per-account STAFF permissions stored in user_permissions. */
@Service
@RequiredArgsConstructor
public class AccountAccessService {
    private final PermissionRepository permissions;

    public boolean canTeach(User user) {
        return active(user) && (hasRole(user, RoleName.MANAGER)
                || hasRole(user, RoleName.STAFF) && user.hasPermission(PermissionCode.TEACH));
    }

    public boolean isDepartmentHead(User user) {
        return active(user) && hasRole(user, RoleName.STAFF)
                && user.hasPermission(PermissionCode.DEPARTMENT_HEAD_PHYSICS);
    }

    public boolean isStaffWithoutTeaching(User user) {
        return user != null && hasRole(user, RoleName.STAFF) && !user.hasPermission(PermissionCode.TEACH);
    }

    /** Replace permissions, or keep valid existing grants when requested is null. */
    public void applyPermissions(User user, String roleName, Collection<String> requested, User grantedBy) {
        RoleName role = RoleName.from(roleName)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unsupported role"));
        if (role != RoleName.STAFF) {
            if (requested != null && !requested.isEmpty())
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only STAFF accounts accept staff permissions");
            user.getPermissions().clear();
            return;
        }

        Set<PermissionCode> target = EnumSet.noneOf(PermissionCode.class);
        if (requested == null) {
            for (PermissionCode code : PermissionCode.values()) {
                if (user.hasPermission(code)) target.add(code);
            }
            if (target.isEmpty()) target.addAll(PermissionCode.defaults());
        } else {
            for (String value : requested) {
                PermissionCode code = PermissionCode.from(value)
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid permission: " + value));
                target.add(code);
            }
            if (target.isEmpty())
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "STAFF needs at least one permission: TEACH or DEPARTMENT_HEAD_PHYSICS");
        }

        user.getPermissions().removeIf(grant -> PermissionCode.from(grant.code()).filter(target::contains).isEmpty());
        List<String> missing = target.stream().filter(code -> !user.hasPermission(code)).map(Enum::name).toList();
        if (missing.isEmpty()) return;
        List<Permission> catalog = permissions.findByCodeIn(missing);
        if (catalog.size() != missing.size())
            throw new IllegalStateException("Permission catalog is incomplete; apply migration V6");
        Integer grantor = grantedBy == null ? null : grantedBy.getId();
        catalog.forEach(permission -> user.grantPermission(permission, grantor));
    }

    private boolean active(User user) {
        return user != null && user.isEnabled() && Boolean.TRUE.equals(user.getActive());
    }

    private boolean hasRole(User user, RoleName role) {
        return user.getRole() != null && role.matches(user.getRole().getName());
    }
}
