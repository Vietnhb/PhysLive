package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceRoleTest {

    @Mock private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsOneOfTheAllowedRoles() {
        User reviewer = authenticate("reviewer@example.com", "REVIEWER");
        CurrentUserService service = new CurrentUserService(userRepository);

        assertThat(service.requireAnyRole("REVIEWER", "ADMIN")).isSameAs(reviewer);
    }

    @Test
    void rejectsAuthenticatedUserWithoutRequiredRole() {
        authenticate("guest@example.com", "GUEST");
        CurrentUserService service = new CurrentUserService(userRepository);

        assertThatThrownBy(() -> service.requireAnyRole("REVIEWER", "ADMIN"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("REVIEWER");
    }

    private User authenticate(String email, String roleName) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, java.util.List.of()));
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        return user;
    }
}
