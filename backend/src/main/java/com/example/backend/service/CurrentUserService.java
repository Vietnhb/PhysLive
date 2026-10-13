package com.example.backend.service;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.backend.entity.User;
import com.example.backend.entity.enums.RoleName;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            return null;
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findByEmail(authentication.getName())
                .filter(user -> user.isEnabled() && Boolean.TRUE.equals(user.getActive()))
                .filter(user -> user.getSchool() == null || user.getSchool().isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    public User requireAnyRole(String... allowedRoles) {
        User user = requireCurrentUser();
        String role = user.getRole() == null ? null : user.getRole().getName();
        RoleName actualRole = RoleName.from(role).orElse(null);
        boolean allowed = actualRole != null && Arrays.stream(allowedRoles)
                .map(RoleName::from)
                .flatMap(java.util.Optional::stream)
                .anyMatch(candidate -> candidate == actualRole);
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Required role: " + String.join(" or ", allowedRoles));
        }
        return user;
    }
}
