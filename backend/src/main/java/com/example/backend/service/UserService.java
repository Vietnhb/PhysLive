package com.example.backend.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.CreateUserRequest;
import com.example.backend.dto.UpdateProfileRequest;
import com.example.backend.dto.UpdateUserRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;

import lombok.AllArgsConstructor;

/**
 * User management service providing full CRUD, flexible email search,
 * BCrypt password hashing, RBAC validation, and ownership enforcement.
 */
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.backend.service.admin.AdminService managedAccounts;

    // ------------------------------------------------------------------ //
    // Read operations
    // ------------------------------------------------------------------ //

    /**
     * Returns profile for the currently authenticated user.
     *
     * @param email resolved from JWT subject
     */
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        return toResponse(user);
    }

    /**
     * Updates personal profile of the currently authenticated user (Self-service).
     *
     * @param email   resolved from JWT subject
     * @param request profile update fields
     */
    @Transactional
    public UserResponse updateMyProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getInstitutionId() != null) {
            if (!java.util.Objects.equals(request.getInstitutionId().trim(), user.getInstitutionId()))
                throw new ApiException(HttpStatus.FORBIDDEN, "Only an administrator can change school membership");
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Lists users with optional email substring search (ADMIN only).
     * Search is case-insensitive keyword match.
     *
     * @param emailSearch keyword or partial email to filter (optional)
     */
    public List<UserResponse> getUsers(String emailSearch) {
        List<User> users;
        if (emailSearch != null && !emailSearch.trim().isEmpty()) {
            users = userRepository.findByEmailContainingIgnoreCase(emailSearch.trim());
        } else {
            users = userRepository.findAll();
        }
        return users.stream().map(this::toResponse).toList();
    }

    /**
     * Returns a user by ID with ownership verification.
     * ADMIN can view any user; regular users can only view their own record.
     *
     * @param id           target user ID
     * @param currentEmail email from JWT
     * @param isAdmin      whether current user has ROLE_ADMIN
     */
    public UserResponse getUserById(Integer id, String currentEmail, boolean isAdmin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với id: " + id));

        // Ownership check: non-admin can only access own profile
        if (!isAdmin && !user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập thông tin của người dùng khác");
        }

        return toResponse(user);
    }

    // ------------------------------------------------------------------ //
    // Write operations
    // ------------------------------------------------------------------ //

    /**
     * Provisions a new user account (ADMIN only).
     * Password is encrypted using BCrypt.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.getRoleId() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required");
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid role"));
        var created = managedAccounts.createUser(new com.example.backend.dto.admin.CreateManagedUserRequest(
                request.getEmail(), request.getPassword(), request.getFullName(), role.getName(), request.getInstitutionId()));
        if (Boolean.FALSE.equals(request.getEnabled())) managedAccounts.setActive(created.id(), false);
        User user = userRepository.findById(created.id()).orElseThrow();
        user.setAvatarUrl(request.getAvatarUrl());
        return toResponse(user);
    }

    /**
     * Updates an existing user's profile or credentials.
     * - Regular users can only update their OWN profile (fullName, institutionId, password).
     * - Only ADMIN can alter roleId and enabled state.
     *
     * @param id           target user ID
     * @param request      update payload
     * @param currentEmail email from JWT
     * @param isAdmin      whether caller is ADMIN
     */
    @Transactional
    public UserResponse updateUser(Integer id, UpdateUserRequest request, String currentEmail, boolean isAdmin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với id: " + id));

        // Ownership check
        if (!isAdmin && !user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa thông tin của người dùng khác");
        }

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (!isAdmin && request.getInstitutionId() != null
                && !java.util.Objects.equals(request.getInstitutionId().trim(), user.getInstitutionId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Only an administrator can change school membership");
        if (isAdmin && (request.getRoleId() != null || request.getInstitutionId() != null)) {
            Role role = request.getRoleId() == null ? user.getRole() : roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid role"));
            managedAccounts.updateUser(id, new com.example.backend.service.admin.AdminService.UpdateUserRequest(
                    user.getFullName(), role.getName(), request.getInstitutionId() == null
                    ? user.getInstitutionId() : request.getInstitutionId()));
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Role update - ADMIN only
        if (request.getRoleId() != null) {
            if (!isAdmin) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ ADMIN mới có quyền thay đổi role");
            }
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Role ID không hợp lệ: " + request.getRoleId()));
            // Role and school invariants are enforced by managedAccounts above.
        }

        // Account state update - ADMIN only with self-disable protection
        if (request.getEnabled() != null) {
            if (!isAdmin) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ ADMIN mới có quyền thay đổi trạng thái tài khoản");
            }
            if (user.getEmail().equalsIgnoreCase(currentEmail) && !request.getEnabled()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN không thể tự vô hiệu hoá tài khoản của chính mình");
            }
            managedAccounts.setActive(id, request.getEnabled());
        }

        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Deactivates or removes a user account (ADMIN only).
     * Self-deletion is strictly prohibited.
     * Safe-deletion handles foreign key constraints from simulations / library_items.
     */
    @Transactional
    public void deleteUser(Integer id, String currentEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với id: " + id));

        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN không thể tự xoá tài khoản của chính mình");
        }

        managedAccounts.setActive(id, false);
    }

    /**
     * Toggles account enabled state (ADMIN only) with self-lockout prevention.
     */
    @Transactional
    public UserResponse updateAccountState(Integer id, boolean enabled, String currentEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với id: " + id));

        if (user.getEmail().equalsIgnoreCase(currentEmail) && !enabled) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADMIN không thể tự vô hiệu hoá tài khoản của chính mình");
        }

        managedAccounts.setActive(id, enabled);
        return toResponse(user);
    }

    // ------------------------------------------------------------------ //
    // Helper
    // ------------------------------------------------------------------ //

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole() != null ? user.getRole().getId() : null,
                user.getRole() != null ? user.getRole().getName() : null,
                user.isEnabled(),
                user.getActive(),
                user.getInstitutionId(),
                user.getLastLogin(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt());
        response.setSchoolId(user.getSchool() == null ? null : user.getSchool().getId());
        response.setPermissions(user.permissionCodes());
        return response;
    }
}
