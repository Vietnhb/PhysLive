package com.example.backend.system.school.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.model.entity.Role;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.repository.UserRepository;
import com.example.backend.system.account.service.AccountAccessService;
import com.example.backend.system.account.service.CurrentUserService;
import com.example.backend.system.account.service.RoleValidationService;
import com.example.backend.system.school.model.entity.School;
import com.example.backend.system.school.model.entity.SchoolClass;
import com.example.backend.system.school.repository.*;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchoolClassRestoreTest {
    private final SchoolClassRepository classes = mock(SchoolClassRepository.class);
    private final ClassEnrollmentRepository enrollments = mock(ClassEnrollmentRepository.class);
    private final ClassTeacherAssignmentRepository assignments = mock(ClassTeacherAssignmentRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final RoleValidationService roles = mock(RoleValidationService.class);
    private final LicenseCheckService license = mock(LicenseCheckService.class);
    private final SchoolClassService service = new SchoolClassService(classes, enrollments, assignments, schools,
            mock(UserRepository.class), currentUser, roles, license, mock(AccountAccessService.class), mock(EntityManager.class));
    private final UUID schoolId = UUID.randomUUID(), classId = UUID.randomUUID();
    private final SchoolClass classroom = new SchoolClass();
    private final User actor = new User();

    @BeforeEach
    void setup() {
        School school = new School(); school.setId(schoolId);
        Role role = new Role(); role.setName("SCHOOL"); actor.setRole(role); actor.setSchool(school);
        classroom.setId(classId); classroom.setSchool(school); classroom.setName("10A1"); classroom.setGradeLevel(10);
        classroom.setSchoolYear("2026-2027"); classroom.setIsActive(false);
        when(currentUser.requireCurrentUser()).thenReturn(actor);
        when(roles.canManageSchool(actor, schoolId)).thenReturn(true);
        when(schools.findByIdForUpdate(schoolId)).thenReturn(Optional.of(school));
        when(classes.findById(classId)).thenReturn(Optional.of(classroom));
    }

    @Test
    void restoresSameClassWithoutReactivatingOldTeacherOrStudentMemberships() {
        var restored = service.restore(schoolId, classId);
        assertTrue(restored.active()); assertEquals(classId, restored.id());
        assertEquals("2026-2027", restored.schoolYear());
        assertEquals(0, restored.teacherCount()); assertEquals(0, restored.studentCount());
        verify(classes).save(classroom);
        verify(enrollments, never()).save(any()); verify(assignments, never()).save(any());
        var order = inOrder(license, schools, classes);
        order.verify(license).requireWriteAccess(actor);
        order.verify(schools).findByIdForUpdate(schoolId);
        order.verify(classes).findById(classId);
    }

    @Test
    void repeatedRestoreDoesNotResetAnAlreadyActiveClass() {
        classroom.setIsActive(true);
        assertTrue(service.restore(schoolId, classId).active());
        verify(classes, never()).save(any());
        verify(enrollments, never()).save(any()); verify(assignments, never()).save(any());
    }

    @Test
    void cannotRestoreClassBelongingToAnotherSchool() {
        School other = new School(); other.setId(UUID.randomUUID()); classroom.setSchool(other);
        assertThrows(ApiException.class, () -> service.restore(schoolId, classId));
        assertFalse(classroom.getIsActive()); verify(classes, never()).save(any());
    }

    @Test
    void deniedSchoolAccessCannotListOrRestoreArchivedClasses() {
        when(roles.canManageSchool(actor, schoolId)).thenReturn(false);
        assertThrows(ApiException.class, () -> service.list(schoolId, true));
        assertThrows(ApiException.class, () -> service.restore(schoolId, classId));
        verifyNoInteractions(classes, schools);
    }

    @Test
    void readOnlyLicenceCannotRestoreClass() {
        doThrow(ApiException.forbidden("Gói dịch vụ không cho phép cập nhật.")).when(license).requireWriteAccess(actor);
        assertThrows(ApiException.class, () -> service.restore(schoolId, classId));
        assertFalse(classroom.getIsActive()); verifyNoInteractions(classes, schools);
    }

    @Test
    void defaultListStaysActiveOnlyWhileManagementCanRequestInactiveClasses() {
        when(classes.findBySchoolIdAndIsActiveTrue(schoolId)).thenReturn(List.of());
        when(classes.findBySchoolIdOrderByNameAsc(schoolId)).thenReturn(List.of(classroom));
        assertTrue(service.list(schoolId).isEmpty());
        assertFalse(service.list(schoolId, true).getFirst().active());
    }
}
