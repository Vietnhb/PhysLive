package com.example.backend.service.school;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.backend.entity.*;
import com.example.backend.entity.school.School;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.school.SchoolRepository;
import com.example.backend.repository.audit.TokenUsageAuditRepository;
import com.example.backend.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchoolTokenPolicyTest {
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final TokenUsageAuditRepository audits = mock(TokenUsageAuditRepository.class);
    private final LicenseCheckService license = new LicenseCheckService();
    private final SchoolService service = new SchoolService(schools, current, license, audits);
    private final ObjectMapper json = new ObjectMapper();
    private School school;
    private User teacher;

    @BeforeEach void setup() {
        school = new School(); school.setId(UUID.randomUUID());
        school.setLicenseStart(LocalDate.now().minusDays(1)); school.setLicenseEnd(LocalDate.now().plusDays(1));
        school.setTokenUsageMonth(LocalDate.now().withDayOfMonth(1)); school.setMonthlyTokenQuota(100);
        Role role = new Role(); role.setName("STAFF");
        teacher = new User(); teacher.setRole(role); teacher.setSchool(school);
        when(current.requireCurrentUser()).thenReturn(teacher);
        when(schools.findByIdForUpdate(school.getId())).thenReturn(Optional.of(school));
    }

    @Test void chargesFullInFlightCallThenBlocksNextCall() {
        school.setUsedTokens(90L);
        service.meterAiCall(() -> json.createObjectNode().set("usage", json.createObjectNode().put("total_tokens", 25)));
        assertThat(school.getUsedTokens()).isEqualTo(115);
        verify(audits).save(argThat(a -> a.getTokens() == 25 && a.getSchool() == school));
        assertThatThrownBy(() -> service.meterAiCall(() -> { throw new AssertionError("Provider must not be called"); }))
                .isInstanceOf(ApiException.class).hasMessageContaining("quota");
    }
    @Test void resetsOnlyMonthlyUsageNotAllowance() {
        school.setTokenUsageMonth(LocalDate.now().minusMonths(1).withDayOfMonth(1)); school.setUsedTokens(1000L);
        service.meterAiCall(() -> json.createObjectNode().set("usage", json.createObjectNode().put("total_tokens", 7)));
        assertThat(school.getUsedTokens()).isEqualTo(7); assertThat(school.getMonthlyTokenQuota()).isEqualTo(100);
    }
    @Test void rejectsMissingProviderUsageWithoutInventingTokens() {
        assertThatThrownBy(() -> service.meterAiCall(json::createObjectNode)).isInstanceOf(ApiException.class);
        verifyNoInteractions(audits);
    }
    @Test void expiredAndFutureLicenseAreReadOnly() {
        school.setLicenseEnd(LocalDate.now().minusDays(1));
        assertThat(license.status(teacher).isActive()).isFalse();
        assertThatThrownBy(() -> service.meterAiCall(json::createObjectNode)).isInstanceOf(ApiException.class);
        school.setLicenseStart(LocalDate.now().plusDays(1)); school.setLicenseEnd(LocalDate.now().plusDays(10));
        assertThat(license.canPerformWriteOperations(teacher)).isFalse();
    }
    @Test void disabledAccountCannotUseActiveSchoolLicense() {
        teacher.setEnabled(false); assertThat(license.canPerformWriteOperations(teacher)).isFalse();
    }
    @Test void curriculumReviewerCannotCallTeachingAi() {
        teacher.getRole().setName("REVIEWER"); teacher.setSchool(null);
        assertThatThrownBy(() -> service.meterAiCall(json::createObjectNode)).isInstanceOf(ApiException.class);
    }
}
