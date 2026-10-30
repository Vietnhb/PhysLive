package com.example.backend.system.school.service;

import com.example.backend.system.operations.service.AdminOperationsService;
import com.example.backend.system.account.model.entity.Role;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.repository.UserRepository;
import com.example.backend.system.school.model.entity.LicensePlan;
import com.example.backend.system.school.model.entity.School;
import com.example.backend.system.school.repository.LicensePlanRepository;
import com.example.backend.system.school.repository.SchoolRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchoolSubscriptionDetailsTest {
    @Test
    void returnsCurrentSchoolPlanAndManagerInsteadOfInferringFromPaymentHistory() {
        var schools = mock(SchoolRepository.class);
        var plans = mock(LicensePlanRepository.class);
        var users = mock(UserRepository.class);
        var quota = mock(AiQuotaService.class);
        var service = new AdminOperationsService(schools, null, null, plans, null, users, quota);
        UUID id = UUID.randomUUID();
        School school = new School(); school.setId(id); school.setPlanCode("STARTER"); school.setUsedTokens(9999L);
        LicensePlan plan = new LicensePlan(); plan.setName("Gói khởi đầu");
        Role schoolRole = new Role(); schoolRole.setName("SCHOOL");
        Role staffRole = new Role(); staffRole.setName("STAFF");
        User manager = new User(); manager.setId(1); manager.setRole(schoolRole); manager.setEmail("school@example.test"); manager.setActive(true);
        User teacher = new User(); teacher.setId(2); teacher.setRole(staffRole);
        when(schools.findById(id)).thenReturn(Optional.of(school));
        when(plans.findById("STARTER")).thenReturn(Optional.of(plan));
        when(users.findBySchoolId(id)).thenReturn(List.of(manager, teacher));
        when(users.countActiveStudents(id)).thenReturn(30L);
        when(quota.usedThisMonth(school)).thenReturn(0L);
        when(quota.currentMonth()).thenReturn(LocalDate.of(2026, 10, 1));
        var result = service.schoolSubscription(id);
        assertEquals("Gói khởi đầu", result.planName());
        assertEquals(1, result.managers().size());
        assertEquals("school@example.test", result.managers().get(0).email());
        assertEquals(30, result.studentsUsed());
        assertEquals(0, result.tokensUsed(), "A past month's stored token count must not leak into this month's usage");
        assertEquals(LocalDate.of(2026, 10, 1), result.usageMonth());
    }
}
