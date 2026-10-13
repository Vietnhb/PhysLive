package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.backend.entity.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StaffAccessTest {
    private User staff(String permission) {
        var role = new Role(); role.setName("STAFF");
        var user = new User(); user.setRole(role);
        var grant = new Permission(permission); grant.setRole(role);
        user.getPermissions().add(new PermissionGrant(grant, null, Instant.now()));
        return user;
    }

    @Test void departmentHeadCannotTeachWithoutGrant() {
        assertThat(StaffAccess.canTeach(staff("DEPARTMENT_HEAD_PHYSICS"))).isFalse();
    }
    @Test void teacherGrantAllowsTeaching() {
        assertThat(StaffAccess.canTeach(staff("TEACH"))).isTrue();
    }
    @Test void permissionFromAnotherRoleIsRejected() {
        var user = staff("TEACH");
        var reviewer = new Role(); reviewer.setName("CONTENT_REVIEWER");
        user.getPermissions().iterator().next().getPermission().setRole(reviewer);
        assertThat(StaffAccess.canTeach(user)).isFalse();
        assertThat(user.permissionCodes()).isEmpty();
    }
}
