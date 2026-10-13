package com.example.backend.security;

import com.example.backend.exception.ApiException;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.AccountAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Enforces STAFF permissions in addition to the HTTP role gates. */
@Component
@RequiredArgsConstructor
public class AccountCapabilityInterceptor implements HandlerInterceptor {
    public static final String[] PATHS = {
        "/api/problems/**", "/api/specifications/**", "/api/validations/**",
        "/api/simulation/**", "/api/simulations/**", "/api/assignments/**",
        "/api/user/students", "/api/library", "/api/library/**"
    };
    private static final List<String> READ_METHODS = List.of("GET", "HEAD", "OPTIONS");

    private final AccountAccessService access;
    private final CurrentUserService currentUser;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getServletPath();
        boolean read = READ_METHODS.contains(request.getMethod());
        if (teaching(path, read) && access.isStaffWithoutTeaching(currentUser.currentUserOrNull()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is not granted the TEACH permission");
        return true;
    }

    private static boolean teaching(String path, boolean read) {
        if (path.startsWith("/api/problems") || path.startsWith("/api/specifications")
                || path.startsWith("/api/validations") || path.startsWith("/api/simulation")
                || path.startsWith("/api/assignments") || path.equals("/api/user/students")
                || path.startsWith("/api/library/folders")) return true;
        return path.startsWith("/api/library") && !read;
    }
}
