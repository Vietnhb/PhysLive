package com.example.backend.system.school.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.repository.UserRepository;
import com.example.backend.system.account.service.CurrentUserService;
import com.example.backend.system.account.service.RoleValidationService;
import com.example.backend.system.school.model.entity.School;
import com.example.backend.system.school.model.entity.SchoolClass;
import com.example.backend.system.school.model.entity.TokenUsageAudit;
import com.example.backend.system.school.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchoolReportServiceTest {
    private final UUID schoolId = UUID.randomUUID();
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final AiQuotaService quota = mock(AiQuotaService.class);
    private final SchoolClassRepository classes = mock(SchoolClassRepository.class);
    private final ClassEnrollmentRepository enrollments = mock(ClassEnrollmentRepository.class);
    private final ClassTeacherAssignmentRepository teachers = mock(ClassTeacherAssignmentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final RoleValidationService roles = mock(RoleValidationService.class);
    private final TokenUsageAuditRepository audits = mock(TokenUsageAuditRepository.class);
    private final SchoolReportService service = new SchoolReportService(schools, quota, classes, enrollments, teachers, users, current, roles, audits);
    private final User actor = new User();

    @BeforeEach
    void setup() {
        when(current.requireCurrentUser()).thenReturn(actor);
        when(roles.canManageSchool(actor, schoolId)).thenReturn(true);
    }

    @Test
    void summaryExplainsCurrentMonthAndReturnsActualLicenceAndStudentQuota() {
        School school = new School(); school.setId(schoolId); school.setName("School"); school.setStudentQuota(100);
        school.setPlanCode("STARTER"); school.setLicenseStart(LocalDate.now().minusDays(1)); school.setLicenseEnd(LocalDate.now().plusYears(1));
        school.setUsedTokens(5000L); school.setMonthlyTokenQuota(null);
        when(schools.findById(schoolId)).thenReturn(Optional.of(school));
        when(quota.usedThisMonth(school)).thenReturn(0L);
        when(quota.currentMonth()).thenReturn(LocalDate.of(2026, 10, 1));
        when(users.countActiveStudents(schoolId)).thenReturn(91L);
        when(enrollments.countActiveStudentsBySchoolId(schoolId)).thenReturn(90L);
        var summary = service.summary(schoolId);
        assertEquals(0, summary.usedTokens()); assertEquals(LocalDate.of(2026, 10, 1), summary.usageMonth());
        assertEquals(100, summary.studentQuota()); assertEquals("ACTIVE", summary.licenseStatus());
        assertEquals(91, summary.students()); assertEquals(90, summary.enrolledStudents());
        assertNull(summary.tokenQuota());
    }

    @Test
    void historyPagesPastOldTwoHundredLimitAndKeepsTotalForPaging() {
        User user = new User(); user.setEmail("teacher@example.test");
        TokenUsageAudit audit = new TokenUsageAudit(); audit.setUser(user); audit.setTokens(2500); audit.setOperation("AI_GENERATION"); audit.setUsageMonth(LocalDate.of(2026, 9, 1));
        when(audits.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(audit), invocation.getArgument(1), 526));
        var result = service.tokenPage(schoolId, 8, 25, "teacher", "AI_GENERATION", "2026-09");
        assertEquals(526, result.totalElements()); assertEquals(22, result.totalPages()); assertEquals(8, result.page());
        assertEquals(2500, result.items().getFirst().tokens());
        verify(audits, never()).findTop200BySchoolIdOrderByRecordedAtDesc(any());
        verify(audits).findAll(any(Specification.class), argThat((Pageable page) -> page.getPageNumber() == 8
                && page.getSort().getOrderFor("recordedAt").isDescending() && page.getSort().getOrderFor("id").isDescending()));
    }

    @Test
    void filterSpecificationScopesSchoolMonthOperationAndLiteralEmailSearch() {
        when(audits.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0, 10)));
        service.tokenPage(schoolId, 0, 10, "TEACH_ER%", "MODEL", "2026-09");
        var captor = org.mockito.ArgumentCaptor.forClass(Specification.class);
        verify(audits).findAll(captor.capture(), any(Pageable.class));
        var root = mock(jakarta.persistence.criteria.Root.class, RETURNS_DEEP_STUBS);
        var cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
        captor.getValue().toPredicate(root, null, cb);
        verify(cb).equal(root.get("school").get("id"), schoolId);
        verify(cb).equal(root.get("usageMonth"), LocalDate.of(2026, 9, 1));
        verify(cb).equal(root.get("operation"), "MODEL");
        verify(cb).like(any(), eq("%teach\\_er\\%%"), eq('\\'));
    }

    @Test
    void metadataIncludesOldMonthsWithoutReadingTheRecentAuditWindow() {
        when(audits.operations(schoolId)).thenReturn(List.of("MODEL"));
        when(audits.months(schoolId)).thenReturn(List.of(LocalDate.of(2025, 1, 1)));
        assertEquals(LocalDate.of(2025, 1, 1), service.tokenOptions(schoolId).months().getFirst());
        verify(audits, never()).findTop200BySchoolIdOrderByRecordedAtDesc(any());
    }

    @Test
    void invalidPagingAndMonthAreRejectedAndOtherSchoolsCannotRead() {
        assertThrows(ApiException.class, () -> service.tokenPage(schoolId, -1, 10, null, null, null));
        assertThrows(ApiException.class, () -> service.tokenPage(schoolId, 0, 101, null, null, null));
        assertThrows(ApiException.class, () -> service.tokenPage(schoolId, 0, 10, null, null, "2026-99"));
        when(roles.canManageSchool(actor, schoolId)).thenReturn(false);
        assertThrows(ApiException.class, () -> service.tokenOptions(schoolId));
        assertThrows(ApiException.class, () -> service.tokenPage(schoolId, 0, 10, null, null, null));
        verifyNoInteractions(audits);
    }

    @Test
    void csvFilteringUsesSameGradeYearAndNameAsClassTable() {
        SchoolClass a = new SchoolClass(); a.setId(UUID.randomUUID()); a.setName("10A1"); a.setGradeLevel(10); a.setSchoolYear("2026-2027");
        SchoolClass b = new SchoolClass(); b.setId(UUID.randomUUID()); b.setName("11A1"); b.setGradeLevel(11); b.setSchoolYear("2025-2026");
        when(classes.findBySchoolIdAndIsActiveTrue(schoolId)).thenReturn(List.of(a, b));
        var result = service.filteredClasses(schoolId, 10, "2026-2027", "10a");
        assertEquals(1, result.size()); assertEquals(a.getId(), result.getFirst().id());
    }
}
