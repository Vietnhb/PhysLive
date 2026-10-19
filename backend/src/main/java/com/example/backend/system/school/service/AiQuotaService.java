package com.example.backend.system.school.service;

import com.example.backend.config.VnpayProperties;
import com.example.backend.exception.ApiException;
import com.example.backend.integration.ai.AiUsageMeter;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.service.CurrentUserService;
import com.example.backend.system.school.model.entity.School;
import com.example.backend.system.school.model.entity.TokenUsageAudit;
import com.example.backend.system.school.repository.SchoolRepository;
import com.example.backend.system.school.repository.TokenUsageAuditRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A school's monthly AI allowance (its licence plan's token quota): every provider call made by one of its accounts is
 * refused once the month's allowance is spent, and counted when it is made. A null quota is unlimited; accounts that
 * belong to no school (reviewers, managers) are not metered. The month follows the billing time zone.
 */
@Service
@RequiredArgsConstructor
public class AiQuotaService implements AiUsageMeter {
    private final CurrentUserService currentUser;
    private final SchoolRepository schools;
    private final TokenUsageAuditRepository audits;
    private final VnpayProperties vnpay;

    public LocalDate currentMonth() {
        return LocalDate.now(vnpay.zoneId()).withDayOfMonth(1);
    }

    /** What the school has used this month; a count kept for an earlier month is spent and starts again from zero. */
    public long usedThisMonth(School school) {
        return currentMonth().equals(school.getTokenUsageMonth()) && school.getUsedTokens() != null ? school.getUsedTokens() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireAllowance() {
        UUID schoolId = callerSchoolId();
        if (schoolId == null) return;
        School school = schools.findById(schoolId).orElse(null);
        if (school == null || school.getMonthlyTokenQuota() == null) return;
        if (usedThisMonth(school) >= school.getMonthlyTokenQuota())
            throw ApiException.forbidden("Trường đã dùng hết hạn mức AI của tháng này. Hạn mức sẽ được làm mới vào đầu tháng sau, hoặc trường có thể nâng gói bản quyền.");
    }

    /** Kept in a transaction of its own: the provider has spent the tokens even if the caller's work fails afterwards. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String operation, long tokens) {
        User user = currentUser.currentUserOrNull();
        UUID schoolId = user == null || user.getSchool() == null ? null : user.getSchool().getId();
        if (schoolId == null || tokens <= 0) return;
        School school = schools.findByIdForUpdate(schoolId).orElse(null);
        if (school == null) return;
        LocalDate month = currentMonth();
        school.setUsedTokens(usedThisMonth(school) + tokens);
        school.setTokenUsageMonth(month);
        schools.save(school);
        TokenUsageAudit audit = new TokenUsageAudit();
        audit.setSchool(school);
        audit.setUser(user);
        audit.setTokens(tokens);
        audit.setOperation(operation);
        audit.setUsageMonth(month);
        audits.save(audit);
    }

    private UUID callerSchoolId() {
        User user = currentUser.currentUserOrNull();
        return user == null || user.getSchool() == null ? null : user.getSchool().getId();
    }
}
