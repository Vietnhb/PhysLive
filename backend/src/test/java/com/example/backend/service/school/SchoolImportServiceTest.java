package com.example.backend.service.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.dto.admin.UserStatusResponse;
import com.example.backend.dto.school.SchoolImport;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.school.School;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.school.ClassEnrollmentRepository;
import com.example.backend.repository.school.ClassTeacherAssignmentRepository;
import com.example.backend.repository.school.SchoolClassRepository;
import com.example.backend.repository.school.SchoolRepository;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.AccountAccessService;
import com.example.backend.service.account.RoleValidationService;
import com.example.backend.service.admin.AdminService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchoolImportServiceTest {
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final SchoolClassRepository classes = mock(SchoolClassRepository.class);
    private final ClassEnrollmentRepository enrollments = mock(ClassEnrollmentRepository.class);
    private final ClassTeacherAssignmentRepository assignments = mock(ClassTeacherAssignmentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final RoleValidationService roles = mock(RoleValidationService.class);
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final LicenseCheckService license = mock(LicenseCheckService.class);
    private final AdminService admin = mock(AdminService.class);
    private final SchoolClassService classService = mock(SchoolClassService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final SchoolImportService service = new SchoolImportService(schools, classes, enrollments, assignments,
            users, current, roles, access, license, admin, classService, entityManager);
    private final UUID schoolId = UUID.randomUUID();
    private final School school = new School();
    private final User actor = user(1, "SCHOOL", "school@example.edu.vn");

    @BeforeEach void prepare() {
        school.setId(schoolId); school.setName("School"); school.setActive(true); school.setStudentQuota(20);
        actor.setSchool(school);
        when(current.requireCurrentUser()).thenReturn(actor);
        when(roles.canManageSchool(actor, schoolId)).thenReturn(true);
        when(schools.findById(schoolId)).thenReturn(Optional.of(school));
        when(schools.findByIdForUpdate(schoolId)).thenReturn(Optional.of(school));
        when(classes.findBySchoolIdAndIsActiveTrue(schoolId)).thenReturn(List.of());
    }

    @Test void previewNormalizesEmailAndGeneratesMissingInitialPassword() {
        SchoolImport.Preview preview = service.preview(schoolId, new SchoolImport.Request(SchoolImport.Kind.USERS,
                List.of(userRow("NEW@EXAMPLE.EDU.VN"))));

        assertThat(preview.canCommit()).isTrue();
        assertThat(preview.rows().getFirst().data().get("email")).isEqualTo("new@example.edu.vn");
        assertThat(preview.rows().getFirst().data().get("initialPassword")).hasSizeGreaterThanOrEqualTo(8);
        assertThat(preview.previewToken()).isNotBlank();
        verifyNoInteractions(admin, classService);
    }

    @Test void commitRequiresTheExactSignedPreview() {
        SchoolImport.Preview preview = service.preview(schoolId, new SchoolImport.Request(SchoolImport.Kind.USERS,
                List.of(userRow("new@example.edu.vn"))));
        assertThatThrownBy(() -> service.commit(schoolId,
                new SchoolImport.Request(SchoolImport.Kind.USERS, List.of(userRow("new@example.edu.vn")))))
                .isInstanceOf(ApiException.class);

        Map<String, String> edited = new java.util.HashMap<>(preview.rows().getFirst().data());
        edited.put("email", "edited@example.edu.vn");
        assertThatThrownBy(() -> service.commit(schoolId, new SchoolImport.Request(SchoolImport.Kind.USERS,
                List.of(new SchoolImport.Row(2, edited)), preview.previewToken()))).isInstanceOf(ApiException.class);
        verifyNoInteractions(admin, classService);
    }

    @Test void reviewedUsersAreCreatedWithSchoolDateOfBirthAndTemporaryPassword() {
        SchoolImport.Preview preview = service.preview(schoolId, new SchoolImport.Request(SchoolImport.Kind.USERS,
                List.of(userRow("new@example.edu.vn"))));
        when(admin.createUser(any())).thenReturn(new UserStatusResponse(3, "new@example.edu.vn", "Nguyen An",
                "STUDENT", true, schoolId.toString(), null, LocalDate.of(2010, 9, 15), List.of(), true));

        SchoolImport.Result result = service.commit(schoolId, reviewed(preview));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.credentials()).singleElement().satisfies(item -> {
            assertThat(item.email()).isEqualTo("new@example.edu.vn");
            assertThat(item.dateOfBirth()).isEqualTo(LocalDate.of(2010, 9, 15));
            assertThat(item.initialPassword()).isNotBlank();
        });
        verify(admin).createUser(any());
    }

    @Test void departmentHeadCannotImportUsersOrClasses() {
        actor.getRole().setName("STAFF");
        when(roles.canManageSchool(actor, schoolId)).thenReturn(false);
        when(access.isDepartmentHead(actor)).thenReturn(true);

        assertThatThrownBy(() -> service.preview(schoolId,
                new SchoolImport.Request(SchoolImport.Kind.USERS, List.of(userRow("a@example.edu.vn")))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.template(schoolId, SchoolImport.Kind.CLASSES)).isInstanceOf(ApiException.class);
    }

    @Test void duplicateEmailsAndStudentQuotaBlockTheWholeBatch() {
        school.setStudentQuota(1);
        SchoolImport.Preview preview = service.preview(schoolId, new SchoolImport.Request(SchoolImport.Kind.USERS,
                List.of(userRow("same@example.edu.vn"), new SchoolImport.Row(3, userRow("SAME@example.edu.vn").data()))));

        assertThat(preview.canCommit()).isFalse();
        assertThat(preview.invalidRows()).isEqualTo(2);
        assertThat(preview.previewToken()).isNull();
    }

    private SchoolImport.Row userRow(String email) {
        return new SchoolImport.Row(2, Map.of("fullName", "Nguyen An", "dateOfBirth", "2010-09-15",
                "email", email, "initialPassword", "", "role", "STUDENT"));
    }
    private SchoolImport.Request reviewed(SchoolImport.Preview preview) {
        return new SchoolImport.Request(preview.kind(), preview.rows().stream()
                .map(row -> new SchoolImport.Row(row.row(), row.data())).toList(), preview.previewToken());
    }
    private User user(int id, String roleName, String email) {
        Role role = new Role(); role.setName(roleName);
        User user = new User(); user.setId(id); user.setRole(role); user.setEmail(email);
        user.setFullName("Nguyen An"); user.setEnabled(true); user.setActive(true);
        return user;
    }
}
