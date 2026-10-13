package com.example.backend.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.PermissionRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountAccessServiceTest {
    private final AccountAccessService access = access();

    @Test
    void newStaffDefaultsToTeachingPermission() {
        User staff = user("STAFF");
        access.applyPermissions(staff, "STAFF", null, user("SCHOOL"));

        assertThat(staff.permissionCodes()).containsExactly("TEACH");
        assertThat(access.canTeach(staff)).isTrue();
        assertThat(access.isDepartmentHead(staff)).isFalse();
    }

    @Test
    void departmentHeadPermissionIsIndependentFromTeaching() {
        User staff = user("STAFF");
        access.applyPermissions(staff, "STAFF", List.of("DEPARTMENT_HEAD_PHYSICS"), user("SCHOOL"));

        assertThat(access.isDepartmentHead(staff)).isTrue();
        assertThat(access.canTeach(staff)).isFalse();
        assertThat(access.isStaffWithoutTeaching(staff)).isTrue();
    }

    @Test
    void explicitStaffPermissionSetCannotBeEmptyOrAppliedToAnotherRole() {
        assertThatThrownBy(() -> access.applyPermissions(user("STAFF"), "STAFF", List.of(), user("SCHOOL")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> access.applyPermissions(user("STUDENT"), "STUDENT", List.of("TEACH"), user("SCHOOL")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void changingAwayFromStaffRemovesGrantsAndNewGrantsRecordActor() {
        User grantor = user("SCHOOL"); grantor.setId(9);
        User staff = user("STAFF");
        access.applyPermissions(staff, "STAFF", List.of("TEACH", "DEPARTMENT_HEAD_PHYSICS"), grantor);
        assertThat(staff.getPermissions()).allMatch(grant -> grant.getGrantedBy().equals(9) && grant.getGrantedAt() != null);

        access.applyPermissions(staff, "STUDENT", null, grantor);
        assertThat(staff.getPermissions()).isEmpty();
    }

    private static User user(String roleName) {
        Role role = new Role(); role.setName(roleName);
        User user = new User(); user.setRole(role); user.setActive(true); user.setEnabled(true);
        return user;
    }

    private static AccountAccessService access() {
        PermissionRepository catalog = mock(PermissionRepository.class);
        when(catalog.findByCodeIn(anyCollection())).thenAnswer(call -> {
            Collection<String> codes = call.getArgument(0);
            return codes.stream().map(Permission::new).toList();
        });
        return new AccountAccessService(catalog);
    }
}
