package com.example.backend.service.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.school.School;
import com.example.backend.entity.school.SchoolClass;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.school.ClassEnrollmentRepository;
import com.example.backend.repository.school.ClassTeacherAssignmentRepository;
import com.example.backend.repository.school.SchoolClassRepository;
import com.example.backend.repository.school.SchoolRepository;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.AccountAccessService;
import com.example.backend.service.account.RoleValidationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SchoolClassAccessTest {

    private final SchoolClassRepository classes = mock(SchoolClassRepository.class);
    private final ClassEnrollmentRepository enrollments = mock(ClassEnrollmentRepository.class);
    private final ClassTeacherAssignmentRepository teachers = mock(ClassTeacherAssignmentRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final RoleValidationService roleValidation = mock(RoleValidationService.class);
    private final LicenseCheckService license = mock(LicenseCheckService.class);
    private final AccountAccessService accountAccess = mock(AccountAccessService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final SchoolClassService service = new SchoolClassService(
            classes, enrollments, teachers, schools, users, current, roleValidation,
            license, accountAccess, entityManager);
    private final School school = school();

    @Test
    void departmentHeadCanManageClassesOnlyInsideOwnSchool() {
        User head = user(1, "STAFF", school);
        when(current.requireCurrentUser()).thenReturn(head);
        when(accountAccess.isDepartmentHead(head)).thenReturn(true);
        when(classes.findBySchoolIdAndIsActiveTrue(school.getId())).thenReturn(List.of());

        assertThat(service.list(school.getId())).isEmpty();
        assertThatThrownBy(() -> service.list(UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void ordinaryStaffCannotManageSchoolClasses() {
        User teacher = user(1, "STAFF", school);
        when(current.requireCurrentUser()).thenReturn(teacher);

        assertThatThrownBy(() -> service.list(school.getId())).isInstanceOf(ApiException.class);
        verifyNoInteractions(classes, teachers, enrollments);
    }

    @Test
    void assignedTeacherMustHaveTeachPermission() {
        User manager = user(1, "SCHOOL", school);
        User teacher = user(2, "STAFF", school);
        SchoolClass target = schoolClass(school);
        when(current.requireCurrentUser()).thenReturn(manager);
        when(roleValidation.canManageSchool(manager, school.getId())).thenReturn(true);
        when(classes.findById(target.getId())).thenReturn(Optional.of(target));
        when(users.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.assignTeacher(school.getId(), target.getId(), teacher.getId()))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        verify(teachers, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void classCreationLocksSchoolBeforeCheckingDuplicateName() {
        User manager = user(1, "SCHOOL", school);
        SchoolClassService.ClassRequest request = new SchoolClassService.ClassRequest(
                " 10A1 ", 10, "2026-2027", "Physics");
        UUID classId = UUID.randomUUID();
        when(current.requireCurrentUser()).thenReturn(manager);
        when(roleValidation.canManageSchool(manager, school.getId())).thenReturn(true);
        when(schools.findByIdForUpdate(school.getId())).thenReturn(Optional.of(school));
        when(classes.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            SchoolClass saved = invocation.getArgument(0);
            saved.setId(classId);
            when(classes.findById(classId)).thenReturn(Optional.of(saved));
            return saved;
        });
        when(teachers.findByClassIdAndIsActiveTrue(classId)).thenReturn(List.of());
        when(enrollments.findActiveStudentsByClassId(classId)).thenReturn(List.of());

        assertThat(service.create(school.getId(), request).name()).isEqualTo("10A1");

        var order = inOrder(schools, entityManager, classes);
        order.verify(schools).findByIdForUpdate(school.getId());
        order.verify(entityManager).refresh(school, LockModeType.PESSIMISTIC_WRITE);
        order.verify(classes).existsBySchoolIdAndNameIgnoreCaseAndSchoolYear(
                school.getId(), "10A1", "2026-2027");
        order.verify(classes).save(org.mockito.ArgumentMatchers.any());
        verify(license).requireWriteAccess(manager);
    }

    private School school() {
        School result = new School();
        result.setId(UUID.randomUUID());
        result.setName("School");
        result.setActive(true);
        return result;
    }

    private User user(int id, String roleName, School memberSchool) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setSchool(memberSchool);
        user.setEnabled(true);
        user.setActive(true);
        return user;
    }

    private SchoolClass schoolClass(School owner) {
        SchoolClass result = new SchoolClass();
        result.setId(UUID.randomUUID());
        result.setSchool(owner);
        result.setName("10A1");
        result.setGradeLevel(10);
        result.setSchoolYear("2026-2027");
        result.setIsActive(true);
        return result;
    }
}
