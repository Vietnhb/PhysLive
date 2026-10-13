package com.example.backend.service.admin;

import com.example.backend.entity.school.School;
import com.example.backend.repository.school.SchoolRepository;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.RoleValidationService;
import com.example.backend.service.account.AccountAccessService;
import com.example.backend.security.PasswordPolicy;
import com.example.backend.service.school.LicenseCheckService;

import com.example.backend.dto.admin.CreateManagedUserRequest;
import com.example.backend.dto.admin.UserStatusResponse;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.RoleName;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@org.springframework.validation.annotation.Validated
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final RoleValidationService roleValidationService;
    private final AccountAccessService accountAccessService;
    private final LicenseCheckService licenseCheckService;
    private final com.example.backend.repository.school.SchoolRepository schoolRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.example.backend.repository.school.ClassEnrollmentRepository enrollments;
    private final com.example.backend.repository.school.ClassTeacherAssignmentRepository teacherAssignments;

    @Transactional(readOnly = true)
    public List<UserStatusResponse> users() {
        currentUserService.requireAnyRole("MANAGER");
        return userRepository.findAll().stream().map(this::toUser).toList();
    }

    @Transactional
    public UserStatusResponse createUser(@jakarta.validation.Valid CreateManagedUserRequest request) {
        if (userRepository.findByEmail(request.email().trim().toLowerCase(java.util.Locale.ROOT)).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }
        Role role = roleRepository.findByName(request.role().trim().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Role is not supported"));
        var school = resolveSchool(request.institutionId());
        roleValidationService.validateRoleSchoolConsistency(role.getName(), school);
        requireManage(role.getName(), school);
        requireStudentSeat(role.getName(), school);
        PasswordPolicy.requireValid(request.password());
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setMustChangePassword(true);
        user.setFullName(request.fullName().trim());
        user.setRole(role);
        user.setSchool(school); user.setInstitutionId(school == null ? null : school.getId().toString());
        accountAccessService.applyPermissions(user, role.getName(), request.permissions(), currentUserService.requireCurrentUser());
        if (RoleName.SCHOOL.matches(role.getName()))
            roleValidationService.validateSingleSchoolManager(school.getId(), null);
        user.setActive(true);
        return toUser(userRepository.save(user));
    }

    @Transactional
    public UserStatusResponse setActive(Integer id, boolean active) {
        if (!active && currentUserService.requireCurrentUser().getId().equals(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "You cannot suspend your own account");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        requireManage(user.getRole().getName(), user.getSchool());
        if (user.getSchool() != null) schoolRepository.findByIdForUpdate(user.getSchool().getId()).orElseThrow();
        if (active && (!user.isEnabled() || !Boolean.TRUE.equals(user.getActive())))
            requireStudentSeat(user.getRole().getName(), user.getSchool());
        if (active && RoleName.SCHOOL.matches(user.getRole().getName()))
            roleValidationService.validateSingleSchoolManager(user.getSchool().getId(), user.getId());
        user.setActive(active); user.setEnabled(active);
        user.setDeactivatedAt(active ? null : java.time.Instant.now());
        user.setDeactivatedBy(active ? null : currentUserService.requireCurrentUser().getId());
        return toUser(userRepository.save(user));
    }

    private UserStatusResponse toUser(User user) {
        return new UserStatusResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole() == null ? "UNKNOWN" : user.getRole().getName(), !Boolean.FALSE.equals(user.getActive()),
                user.getInstitutionId(), user.getLastLogin(), null, user.permissionCodes(), user.isMustChangePassword());
    }

    @Transactional
    public UserStatusResponse resetPassword(Integer id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        requireManage(user.getRole().getName(), user.getSchool());
        PasswordPolicy.requireValid(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        return toUser(userRepository.save(user));
    }

    public record UpdateUserRequest(@jakarta.validation.constraints.NotBlank String fullName,
            @jakarta.validation.constraints.NotBlank String role, String institutionId,
            java.util.List<String> permissions) {
        public UpdateUserRequest(String fullName, String role, String institutionId) {
            this(fullName, role, institutionId, null);
        }
    }

    @Transactional
    public UserStatusResponse updateUser(Integer id, @jakarta.validation.Valid UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        requireManage(user.getRole().getName(), user.getSchool());
        Role role = roleRepository.findByName(request.role().trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Role is not supported"));
        if (currentUserService.requireCurrentUser().getId().equals(id) && !RoleName.MANAGER.matches(role.getName()))
            throw new ApiException(HttpStatus.CONFLICT, "You cannot remove your own admin role");
        var school = resolveSchool(request.institutionId());
        roleValidationService.validateRoleSchoolConsistency(role.getName(), school);
        requireManage(role.getName(), school);
        boolean alreadyOccupiesSeat = RoleName.STUDENT.matches(user.getRole().getName()) && !Boolean.FALSE.equals(user.getActive())
                && user.getSchool() != null && school != null && user.getSchool().getId().equals(school.getId());
        if (!Boolean.FALSE.equals(user.getActive()) && !alreadyOccupiesSeat) requireStudentSeat(role.getName(), school);
        if (Boolean.TRUE.equals(user.getActive()) && RoleName.SCHOOL.matches(role.getName()))
            roleValidationService.validateSingleSchoolManager(school.getId(), user.getId());
        boolean schoolChanged = !java.util.Objects.equals(user.getSchool() == null ? null : user.getSchool().getId(),
                school == null ? null : school.getId());
        if (schoolChanged || !user.getRole().getName().equalsIgnoreCase(role.getName())) {
            for (var enrollment : enrollments.findActiveEnrollmentsByStudentId(id)) {
                enrollment.setStatus(com.example.backend.entity.school.ClassEnrollment.EnrollmentStatus.DROPPED);
                enrollments.save(enrollment);
            }
            for (var assignment : teacherAssignments.findActiveClassesByTeacherId(id)) {
                assignment.setIsActive(false); teacherAssignments.save(assignment);
            }
            enrollments.flush(); teacherAssignments.flush();
        }
        user.setFullName(request.fullName().trim()); user.setRole(role); user.setSchool(school); user.setInstitutionId(school == null ? null : school.getId().toString());
        accountAccessService.applyPermissions(user, role.getName(), request.permissions(), currentUserService.requireCurrentUser());
        return toUser(userRepository.save(user));
    }

    private void requireManage(String targetRole, com.example.backend.entity.school.School school) {
        User actor = currentUserService.requireCurrentUser();
        if (actor.getRole() != null && RoleName.MANAGER.matches(actor.getRole().getName())) return;
        if (school == null || !roleValidationService.canManageSchool(actor, school.getId())
                || !(RoleName.STAFF.matches(targetRole) || RoleName.STUDENT.matches(targetRole)))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage teachers and students in your school");
        licenseCheckService.requireWriteAccess(actor);
    }

    private void requireStudentSeat(String role, com.example.backend.entity.school.School school) {
        if (!RoleName.STUDENT.matches(role) || school == null) return;
        var locked = schoolRepository.findByIdForUpdate(school.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "School not found"));
        entityManager.refresh(locked, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (locked.getStudentQuota() != null && userRepository.countActiveStudents(locked.getId()) >= locked.getStudentQuota())
            throw new ApiException(HttpStatus.CONFLICT, "Trường đã hết quota học sinh. Vui lòng nâng gói hoặc tạm khóa tài khoản không còn sử dụng.");
    }

    private com.example.backend.entity.school.School resolveSchool(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return schoolRepository.findByIdForUpdate(java.util.UUID.fromString(id))
                    .filter(com.example.backend.entity.school.School::isActive)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Select an active school"));
        } catch (IllegalArgumentException ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "School ID is invalid"); }
    }
}
