package com.example.backend.system.school.service;

import com.example.backend.system.school.dto.SchoolContracts;
import com.example.backend.system.school.model.entity.School;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchoolSubscriptionTest {
    @Test
    void licenseIncludesBothStartAndEndDate() {
        School school = new School();
        school.setActive(true);
        school.setLicenseStart(LocalDate.now());
        school.setLicenseEnd(LocalDate.now());
        school.setPlanCode("STARTER");
        var response = SchoolContracts.Response.from(school);
        assertEquals("ACTIVE", response.subscriptionStatus());
        assertEquals("STARTER", response.planCode());
        assertTrue(response.licenseActive());
    }

    @Test
    void distinguishesUpcomingExpiredAndMissingDates() {
        School school = new School(); school.setActive(true);
        assertEquals("UNLICENSED", SchoolContracts.Response.from(school).subscriptionStatus());
        school.setLicenseStart(LocalDate.now().plusDays(1));
        school.setLicenseEnd(LocalDate.now().plusYears(1));
        assertEquals("UPCOMING", SchoolContracts.Response.from(school).subscriptionStatus());
        school.setLicenseStart(LocalDate.now().minusYears(1));
        school.setLicenseEnd(LocalDate.now().minusDays(1));
        assertEquals("EXPIRED", SchoolContracts.Response.from(school).subscriptionStatus());
    }

    @Test
    void suspendedSchoolIsNotShownAsUsableEvenWithValidLicense() {
        School school = new School(); school.setActive(false);
        school.setLicenseStart(LocalDate.now().minusDays(1));
        school.setLicenseEnd(LocalDate.now().plusDays(1));
        assertEquals("SUSPENDED", SchoolContracts.Response.from(school).subscriptionStatus());
    }
}
