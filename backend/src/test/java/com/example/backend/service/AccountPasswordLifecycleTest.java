package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.admin.AdminService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountPasswordLifecycleTest {

    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserService service = new UserService(
            users, mock(RoleRepository.class), encoder, mock(AdminService.class));

    @Test
    void firstPasswordChangeClearsMandatoryChangeFlag() {
        User user = user("Temporary123", true);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        var response = service.changePassword(
                user.getEmail(), new ChangePasswordRequest("Temporary123", "Permanent456"));

        assertThat(encoder.matches("Permanent456", user.getPassword())).isTrue();
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(response.isMustChangePassword()).isFalse();
        verify(users).save(user);
    }

    @Test
    void wrongCurrentPasswordDoesNotModifyAccount() {
        User user = user("Temporary123", true);
        String originalHash = user.getPassword();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(
                user.getEmail(), new ChangePasswordRequest("WrongPassword", "Permanent456")))
                .isInstanceOf(ApiException.class);

        assertThat(user.getPassword()).isEqualTo(originalHash);
        assertThat(user.isMustChangePassword()).isTrue();
        verify(users, never()).save(user);
    }

    @Test
    void bcryptByteLimitIsEnforced() {
        User user = user("Temporary123", true);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(
                user.getEmail(), new ChangePasswordRequest("Temporary123", "á".repeat(40))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("72-byte");
        verify(users, never()).save(user);
    }

    private User user(String rawPassword, boolean mustChangePassword) {
        Role role = new Role();
        role.setId(1);
        role.setName("STUDENT");
        User user = new User();
        user.setId(10);
        user.setEmail("student@school.test");
        user.setFullName("Student");
        user.setPassword(encoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        user.setActive(true);
        user.setMustChangePassword(mustChangePassword);
        return user;
    }
}
