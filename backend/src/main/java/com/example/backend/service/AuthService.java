package com.example.backend.service;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;

import lombok.AllArgsConstructor;

/**
 * Authentication service handling login and registration.
 *
 * <p>Passwords are always stored as BCrypt hashes; plain-text is never persisted.
 * Account state ({@code enabled}) is checked on every login attempt.
 * {@code lastLogin} is stamped with current UTC time on every successful login.
 */
@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ //
    // Login
    // ------------------------------------------------------------------ //

    /**
     * Validates credentials and returns a signed JWT together with user info.
     *
     * @throws ApiException 401 if email not found or password mismatch
     * @throws ApiException 403 if account is disabled
     */
    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        if (!user.isEnabled() || !Boolean.TRUE.equals(user.getActive())
                || (user.getSchool() != null && !user.getSchool().isActive())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản đã bị vô hiệu hoá. Vui lòng liên hệ quản trị viên.");
        }

        // Stamp last login time
        user.setLastLogin(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getName());

        UserResponse userInfo = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getId(),
                user.getRole().getName(),
                user.isEnabled(),
                user.getActive(),
                user.getInstitutionId(),
                user.getLastLogin(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt());

        userInfo.setSchoolId(user.getSchool() == null ? null : user.getSchool().getId());
        userInfo.setPermissions(user.permissionCodes());
        return new LoginResponse(token, userInfo);
    }

    // ------------------------------------------------------------------ //
    // Sign-up
    // ------------------------------------------------------------------ //

    /**
     * Registers a new user with role {@code GUEST} and a BCrypt-hashed password.
     *
     * @throws ApiException 400 if email is already in use
     * @throws ApiException 500 if the GUEST role is missing from the database
     */
    public void signup(SignupRequest request) {
        throw new ApiException(HttpStatus.FORBIDDEN,
                "Personal signup is disabled. Register a school or contact your school manager.");
    }
}
