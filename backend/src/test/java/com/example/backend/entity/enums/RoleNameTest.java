package com.example.backend.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RoleNameTest {

    @Test
    void exposesSixCanonicalRolesWithStableIdsAndScopes() {
        assertThat(RoleName.values()).containsExactly(
                RoleName.ADMIN, RoleName.MANAGER, RoleName.REVIEWER,
                RoleName.SCHOOL, RoleName.STAFF, RoleName.STUDENT);
        assertThat(Map.of(
                RoleName.ADMIN, RoleName.ADMIN.id(),
                RoleName.MANAGER, RoleName.MANAGER.id(),
                RoleName.REVIEWER, RoleName.REVIEWER.id(),
                RoleName.SCHOOL, RoleName.SCHOOL.id(),
                RoleName.STAFF, RoleName.STAFF.id(),
                RoleName.STUDENT, RoleName.STUDENT.id()))
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        RoleName.ADMIN, 1, RoleName.MANAGER, 2, RoleName.REVIEWER, 3,
                        RoleName.SCHOOL, 4, RoleName.STAFF, 5, RoleName.STUDENT, 6));
        assertThat(RoleName.ADMIN.isPlatformRole()).isTrue();
        assertThat(RoleName.MANAGER.isPlatformRole()).isTrue();
        assertThat(RoleName.REVIEWER.isPlatformRole()).isTrue();
        assertThat(RoleName.SCHOOL.isSchoolRole()).isTrue();
        assertThat(RoleName.STAFF.isSchoolRole()).isTrue();
        assertThat(RoleName.STUDENT.isSchoolRole()).isTrue();
    }

    @Test
    void acceptsLegacyNamesWithoutMakingThemCanonicalRoles() {
        assertThat(RoleName.from("CONTENT_REVIEWER")).contains(RoleName.REVIEWER);
        assertThat(RoleName.from("SCHOOL_MANAGER")).contains(RoleName.SCHOOL);
        assertThat(RoleName.from("TEACHER")).contains(RoleName.STAFF);
        assertThat(RoleName.from("GUEST")).isEmpty();
    }
}
