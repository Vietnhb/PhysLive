package com.example.backend.system.account.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.dto.CreateManagedUserRequest;
import com.example.backend.system.account.dto.UpdateManagedUserRequest;
import com.example.backend.system.account.model.entity.Role;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.repository.RoleRepository;
import com.example.backend.system.account.repository.UserRepository;
import com.example.backend.system.school.repository.SchoolRepository;
import com.example.backend.system.school.service.LicenseCheckService;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminManagerAccountsTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final AdminService service = new AdminService(users, roles, passwords, current, mock(RoleValidationService.class),
            mock(AccountAccessService.class), mock(LicenseCheckService.class), mock(SchoolRepository.class), mock(EntityManager.class));
    private final User admin = new User(), manager = new User();

    private static Role role(String name) { Role role = new Role(); role.setName(name); return role; }

    @BeforeEach
    void setup() {
        admin.setId(1); admin.setRole(role("ADMIN"));
        manager.setId(2); manager.setRole(role("MANAGER")); manager.setEmail("manager@example.test"); manager.setFullName("Manager"); manager.setActive(true);
        when(current.requireCurrentUser()).thenReturn(admin);
        when(users.findById(2)).thenReturn(Optional.of(manager));
        when(roles.findByName("MANAGER")).thenReturn(Optional.of(manager.getRole()));
        when(passwords.encode(anyString())).thenReturn("encoded-password");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void adminCreatesManagerAndRequiresPasswordChange() {
        var created = service.createUser(new CreateManagedUserRequest("new@example.test", "Manager123!", "New Manager", "MANAGER", null));
        assertEquals("MANAGER", created.role());
        verify(users).save(argThat(user -> user.isMustChangePassword() && user.getSchool() == null
                && user.getPassword().equals("encoded-password")));
    }

    @Test
    void adminUpdatesLocksUnlocksAndResetsManagerPassword() {
        assertEquals("Updated", service.updateUser(2, new UpdateManagedUserRequest("Updated", "MANAGER", null)).fullName());
        assertFalse(service.setActive(2, false).active());
        assertTrue(service.setActive(2, true).active());
        service.resetPassword(2, "Temporary123!");
        assertTrue(manager.isMustChangePassword()); assertEquals("encoded-password", manager.getPassword());
    }

    @Test
    void adminCannotManageOtherRolesOrPromoteManagerToAdmin() {
        when(roles.findByName("ADMIN")).thenReturn(Optional.of(admin.getRole()));
        assertThrows(ApiException.class, () -> service.updateUser(2, new UpdateManagedUserRequest("Admin", "ADMIN", null)));
        manager.setRole(role("STUDENT"));
        assertThrows(ApiException.class, () -> service.setActive(2, false));
        assertThrows(ApiException.class, () -> service.resetPassword(2, "Temporary123!"));
        assertThrows(ApiException.class, () -> service.createUser(new CreateManagedUserRequest("new@example.test", "Temporary123!", "Admin", "ADMIN", null)));
    }

    @Test
    void managerCannotCreateAnotherManager() {
        when(current.requireCurrentUser()).thenReturn(manager);
        assertThrows(ApiException.class, () -> service.createUser(new CreateManagedUserRequest("new@example.test", "Temporary123!", "Manager", "MANAGER", null)));
        verify(users, never()).save(any());
    }
}
