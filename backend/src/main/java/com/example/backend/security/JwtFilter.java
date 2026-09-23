package com.example.backend.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

import java.util.Collections;

/**
 * Intercepts every request, extracts the Bearer JWT from the Authorization header,
 * validates it, and populates the {@link SecurityContextHolder} with the user's
 * email and role authority.
 *
 * <p>If the header is absent the filter passes the request through unchanged –
 * Spring Security's own rules will decide whether the endpoint is public or not.
 *
 * <p>If the token is present but invalid, the filter short-circuits with a
 * {@code 401 Unauthorized} JSON response so clients get a machine-readable error
 * instead of a Spring redirect.
 */
@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final com.example.backend.repository.UserRepository users;
    private final com.example.backend.service.school.LicenseCheckService licenses;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token – let the request pass; Spring Security will enforce access rules
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.extractClaims(token);
            String email = claims.getSubject();
            var user = users.findByEmail(email).filter(u -> u.isEnabled() && Boolean.TRUE.equals(u.getActive()))
                    .filter(u -> u.getSchool() == null || u.getSchool().isActive()).orElse(null);
            if (user == null || user.getRole() == null) {
                SecurityContextHolder.clearContext();
                sendUnauthorized(response, "Account or school is disabled");
                return;
            }
            var role = com.example.backend.entity.enums.RoleName.from(user.getRole().getName()).orElse(null);
            String path = request.getRequestURI().substring(request.getContextPath().length());
            boolean write = !java.util.List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
            boolean profile = "/api/user/me".equals(path) || path.startsWith("/api/user/me/");
            boolean renewal = role == com.example.backend.entity.enums.RoleName.SCHOOL_MANAGER
                    && "POST".equals(request.getMethod())
                    && java.util.List.of("/api/school/billing/quote", "/api/school/billing/checkout").contains(path);
            if (write && path.startsWith("/api/") && !path.startsWith("/api/auth/")
                    && !profile && !renewal && !licenses.canPerformWriteOperations(user)) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"status\":403,\"message\":\"School license does not allow writes\"}");
                return;
            }
            var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + (role == null ? user.getRole().getName() : role.name())));
            if (role == com.example.backend.entity.enums.RoleName.CONTENT_REVIEWER)
                authorities.add(new SimpleGrantedAuthority("ROLE_REVIEWER"));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            sendUnauthorized(response, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ------------------------------------------------------------------ //
    // Helper
    // ------------------------------------------------------------------ //

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":401,\"message\":\"" + message + "\"}");
    }
}
