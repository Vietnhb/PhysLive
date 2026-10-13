package com.example.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.school.LicenseCheckService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtFilterPasswordLifecycleTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void temporaryPasswordBlocksNormalApiAccess() throws Exception {
        Fixture fixture = fixture(true);
        var request = request("GET", "/api/assignments");
        var response = new MockHttpServletResponse();

        fixture.filter.doFilter(request, response, fixture.chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PASSWORD_CHANGE_REQUIRED");
        verify(fixture.chain, never()).doFilter(request, response);
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/user/me", "PUT,/api/user/me/password"})
    void temporaryPasswordAllowsOnlyPasswordLifecycleEndpoints(String method, String path) throws Exception {
        Fixture fixture = fixture(true);
        var request = request(method, path);
        var response = new MockHttpServletResponse();

        fixture.filter.doFilter(request, response, fixture.chain);

        verify(fixture.chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void changedPasswordRestoresNormalApiAccess() throws Exception {
        Fixture fixture = fixture(false);
        var request = request("GET", "/api/assignments");
        var response = new MockHttpServletResponse();

        fixture.filter.doFilter(request, response, fixture.chain);

        verify(fixture.chain).doFilter(request, response);
    }

    private Fixture fixture(boolean mustChangePassword) {
        JwtUtil jwt = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("student@school.test");
        when(jwt.extractClaims("token")).thenReturn(claims);

        Role role = new Role();
        role.setName("STUDENT");
        User user = new User();
        user.setEmail("student@school.test");
        user.setRole(role);
        user.setEnabled(true);
        user.setActive(true);
        user.setMustChangePassword(mustChangePassword);

        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        FilterChain chain = mock(FilterChain.class);
        JwtFilter filter = new JwtFilter(jwt, users, mock(LicenseCheckService.class));
        return new Fixture(filter, chain);
    }

    private MockHttpServletRequest request(String method, String path) {
        var request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private record Fixture(JwtFilter filter, FilterChain chain) { }
}
