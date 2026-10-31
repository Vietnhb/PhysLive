package com.example.backend.system.school.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.repository.UserRepository;
import com.example.backend.system.account.service.*;
import com.example.backend.system.school.controller.SchoolClassController;
import com.example.backend.system.school.dataio.service.SchoolClassCsv;
import com.example.backend.system.school.dto.SchoolClassContracts.ClassDetail;
import com.example.backend.system.school.dto.SchoolClassContracts.Person;
import com.example.backend.system.school.model.entity.ClassEnrollment;
import com.example.backend.system.school.model.entity.ClassTeacherAssignment;
import com.example.backend.system.school.model.entity.School;
import com.example.backend.system.school.model.entity.SchoolClass;
import com.example.backend.system.school.repository.*;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchoolClassCsvExportTest {
    private final UUID schoolId = UUID.randomUUID(), classId = UUID.randomUUID();
    private final SchoolClassRepository classes = mock(SchoolClassRepository.class);
    private final ClassEnrollmentRepository enrollments = mock(ClassEnrollmentRepository.class);
    private final ClassTeacherAssignmentRepository assignments = mock(ClassTeacherAssignmentRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final RoleValidationService roles = mock(RoleValidationService.class);
    private final SchoolClass classroom = new SchoolClass();
    private final User actor = new User();
    private final SchoolClassService service = new SchoolClassService(classes, enrollments, assignments,
            mock(SchoolRepository.class), mock(UserRepository.class), current, roles, mock(LicenseCheckService.class),
            mock(AccountAccessService.class), mock(EntityManager.class));
    private final SchoolClassController controller = new SchoolClassController(service);

    @BeforeEach
    void setup() {
        School school = new School(); school.setId(schoolId);
        classroom.setId(classId); classroom.setSchool(school); classroom.setName("12A1"); classroom.setGradeLevel(12);
        classroom.setSchoolYear("2026-2027"); classroom.setSubject("Vật lý"); classroom.setIsActive(true);
        when(current.requireCurrentUser()).thenReturn(actor); when(roles.canManageSchool(actor, schoolId)).thenReturn(true);
        when(classes.findById(classId)).thenReturn(Optional.of(classroom));
    }

    @Test
    void exportsAllThirtyStudentsWithClassAndTeacherMetadataAndUtf8Bom() {
        User teacher = new User(); teacher.setId(99); teacher.setFullName("Nguyễn Văn An"); teacher.setEmail("teacher@example.test");
        ClassTeacherAssignment assignment = new ClassTeacherAssignment(); assignment.setTeacher(teacher);
        when(assignments.findByClassIdAndIsActiveTrue(classId)).thenReturn(List.of(assignment));
        when(enrollments.findActiveStudentsByClassId(classId)).thenReturn(IntStream.rangeClosed(1, 30).mapToObj(index -> {
            User student = new User(); student.setId(index); student.setFullName("Học sinh " + index); student.setEmail("hs" + index + "@example.test");
            ClassEnrollment enrollment = new ClassEnrollment(); enrollment.setStudent(student); return enrollment;
        }).toList());
        var response = controller.exportStudents(schoolId, classId);
        byte[] bytes = response.getBody(); assertNotNull(bytes);
        assertArrayEquals(new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf}, java.util.Arrays.copyOf(bytes, 3));
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(31, csv.lines().count());
        assertTrue(csv.contains("30,\"12A1\",12,\"2026-2027\",\"Vật lý\",\"Nguyễn Văn An\",\"teacher@example.test\",\"Học sinh 30\",\"hs30@example.test\""));
        assertTrue(response.getHeaders().getContentType().toString().contains("charset=UTF-8"));
        assertTrue(response.getHeaders().getContentDisposition().isAttachment());
    }

    @Test
    void deniedSchoolOrForeignClassCannotExport() {
        when(roles.canManageSchool(actor, schoolId)).thenReturn(false);
        assertThrows(ApiException.class, () -> controller.exportStudents(schoolId, classId));
        verifyNoInteractions(enrollments, assignments);
        when(roles.canManageSchool(actor, schoolId)).thenReturn(true);
        School other = new School(); other.setId(UUID.randomUUID()); classroom.setSchool(other);
        assertThrows(ApiException.class, () -> controller.exportStudents(schoolId, classId));
        verifyNoInteractions(enrollments, assignments);
    }

    @Test
    void escapesCommasQuotesLineBreaksAndSpreadsheetFormulas() {
        ClassDetail detail = new ClassDetail(classId, "12A,\"1\"", 12, "2026-2027", null, true, List.of(),
                List.of(new Person(1, " =SUM(1,2)", "hs@example.test"), new Person(2, "Nguyễn\nAn", "a@example.test")));
        String csv = new String(SchoolClassCsv.students(detail), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"12A,\"\"1\"\"\""));
        assertTrue(csv.contains("\"' =SUM(1,2)\""));
        assertTrue(csv.contains("\"Nguyễn\nAn\""));
    }

    @Test
    void archivedClassIsNotExportedAsACurrentRoster() {
        classroom.setIsActive(false);
        assertThrows(ApiException.class, () -> controller.exportStudents(schoolId, classId));
        verifyNoInteractions(enrollments, assignments);
    }
}
