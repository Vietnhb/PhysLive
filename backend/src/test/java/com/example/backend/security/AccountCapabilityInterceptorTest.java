package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.AccountAccessService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AccountCapabilityInterceptorTest {

    @ParameterizedTest
    @CsvSource({
        "DEPARTMENT_HEAD_PHYSICS,POST,/api/simulations/id/runs,false",
        "DEPARTMENT_HEAD_PHYSICS,GET,/api/assignments,false",
        "DEPARTMENT_HEAD_PHYSICS,GET,/api/library/folders,false",
        "DEPARTMENT_HEAD_PHYSICS,GET,/api/library,true",
        "TEACH,POST,/api/simulations/id/runs,true",
        "TEACH,POST,/api/library,true"
    })
    void staffTeachingRoutesRequireTeachPermission(String permission, String method, String path, boolean allowed) {
        User staff = user("STAFF");
        staff.grantPermission(new Permission(permission), null);
        CurrentUserService current = mock(CurrentUserService.class);
        when(current.currentUserOrNull()).thenReturn(staff);
        AccountAccessService access = mock(AccountAccessService.class);
        when(access.isStaffWithoutTeaching(staff)).thenReturn(!"TEACH".equals(permission));
        var interceptor = new AccountCapabilityInterceptor(access, current);
        var request = new MockHttpServletRequest(method, path); request.setServletPath(path);

        if (allowed) assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        else assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ApiException.class);
    }

    @ParameterizedTest
    @CsvSource({"STUDENT,POST,/api/assignments/id/submit", "SCHOOL,GET,/api/library"})
    void nonStaffRolesAreUnaffected(String role, String method, String path) {
        User user = user(role);
        CurrentUserService current = mock(CurrentUserService.class);
        when(current.currentUserOrNull()).thenReturn(user);
        AccountAccessService access = mock(AccountAccessService.class);
        var interceptor = new AccountCapabilityInterceptor(access, current);
        var request = new MockHttpServletRequest(method, path); request.setServletPath(path);

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
    }

    private static User user(String roleName) {
        Role role = new Role(); role.setName(roleName);
        User user = new User(); user.setRole(role); user.setActive(true); user.setEnabled(true);
        return user;
    }
}
