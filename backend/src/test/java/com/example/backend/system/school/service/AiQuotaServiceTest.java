package com.example.backend.system.school.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.config.VnpayProperties;
import com.example.backend.exception.ApiException;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.service.CurrentUserService;
import com.example.backend.system.school.model.entity.School;
import com.example.backend.system.school.model.entity.TokenUsageAudit;
import com.example.backend.system.school.repository.SchoolRepository;
import com.example.backend.system.school.repository.TokenUsageAuditRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** A school's monthly AI allowance: refused when spent, counted per call, started again each month, unlimited when null. */
class AiQuotaServiceTest {
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final TokenUsageAuditRepository audits = mock(TokenUsageAuditRepository.class);
    private final VnpayProperties vnpay = mock(VnpayProperties.class);
    private final AiQuotaService quota = new AiQuotaService(currentUser, schools, audits, vnpay);
    private final School school = new School();
    private final User teacher = new User();

    @BeforeEach
    void setUp() {
        when(vnpay.zoneId()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        school.setId(UUID.randomUUID());
        teacher.setSchool(school);
        when(currentUser.currentUserOrNull()).thenReturn(teacher);
        when(schools.findById(school.getId())).thenReturn(Optional.of(school));
        when(schools.findByIdForUpdate(school.getId())).thenReturn(Optional.of(school));
    }

    @Test
    void aSpentAllowanceRefusesTheNextCall() {
        school.setMonthlyTokenQuota(1000);
        school.setTokenUsageMonth(quota.currentMonth());
        school.setUsedTokens(1000L);
        ApiException refused = assertThrows(ApiException.class, quota::requireAllowance);
        assertEquals(403, refused.getStatus().value());
    }

    @Test
    void lastMonthsUsageDoesNotCountThisMonth() {
        school.setMonthlyTokenQuota(1000);
        school.setTokenUsageMonth(quota.currentMonth().minusMonths(1));
        school.setUsedTokens(5000L);
        assertDoesNotThrow(quota::requireAllowance);

        quota.record("MODEL", 300);
        assertEquals(300L, school.getUsedTokens());
        assertEquals(quota.currentMonth(), school.getTokenUsageMonth());
        ArgumentCaptor<TokenUsageAudit> audit = ArgumentCaptor.forClass(TokenUsageAudit.class);
        verify(audits).save(audit.capture());
        assertEquals("MODEL", audit.getValue().getOperation());
        assertEquals(300L, audit.getValue().getTokens());
    }

    @Test
    void callsAddUpWithinTheMonth() {
        school.setMonthlyTokenQuota(1000);
        school.setTokenUsageMonth(quota.currentMonth());
        school.setUsedTokens(400L);
        quota.record("SCENE", 250);
        assertEquals(650L, school.getUsedTokens());
    }

    @Test
    void aNullQuotaIsUnlimited() {
        school.setMonthlyTokenQuota(null);
        school.setTokenUsageMonth(quota.currentMonth());
        school.setUsedTokens(Long.MAX_VALUE / 2);
        assertDoesNotThrow(quota::requireAllowance);
    }

    @Test
    void anAccountOfNoSchoolIsNotMetered() {
        when(currentUser.currentUserOrNull()).thenReturn(new User());
        assertDoesNotThrow(quota::requireAllowance);
        quota.record("MODEL", 500);
        verify(audits, never()).save(any());
        verify(schools, never()).findByIdForUpdate(any());
    }

    @Test
    void aNewSchoolWithoutAllowanceIsRefused() {
        school.setMonthlyTokenQuota(0);
        assertThrows(ApiException.class, quota::requireAllowance);
    }

    @Test
    void theMonthFollowsTheBillingTimeZone() {
        assertEquals(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).withDayOfMonth(1), quota.currentMonth());
    }
}
