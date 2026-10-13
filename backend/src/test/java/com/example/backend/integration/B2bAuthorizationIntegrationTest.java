package com.example.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.school.School;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.school.SchoolRepository;
import com.example.backend.security.JwtUtil;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false", // H2 schema is created by Hibernate for this test.
        "spring.config.import=",
        "physlive.database.external-enabled=false",
        "SUPABASE_DB_URL=", "DATABASE_URL=", "DIRECT_URL=",
        "spring.datasource.url=jdbc:h2:mem:b2b-security;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "jwt.secret=b2b-test-only-signing-key-at-least-32-bytes",
        "physlive.vnpay.tmn-code=", "physlive.vnpay.hash-secret=",
        "OPENROUTER_API_KEY=", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
@Transactional
class B2bAuthorizationIntegrationTest {
    @Autowired MockMvc http;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired SchoolRepository schools;
    @Autowired JwtUtil jwt;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired com.example.backend.repository.school.ClassEnrollmentRepository enrollments;
    private School school;
    private User manager;

    @BeforeEach void setup() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:b2b-security");
        }
        school = school("A");
        manager = user("SCHOOL_MANAGER", school);
        role("TEACHER"); role("STUDENT"); role("ADMIN");
    }
    private School school(String label) {
        School s = new School(); String code = label + UUID.randomUUID();
        s.setCode(code); s.setName(code); s.setLicenseStart(LocalDate.now().minusDays(1));
        s.setLicenseEnd(LocalDate.now().plusDays(20)); s.setMonthlyTokenQuota(100); s.setStudentQuota(3);
        return schools.saveAndFlush(s);
    }
    private Role role(String name) {
        return roles.findByName(name).orElseGet(() -> { Role r = new Role(); r.setName(name); return roles.saveAndFlush(r); });
    }
    private User user(String name, School s) {
        User u = new User(); u.setEmail(UUID.randomUUID() + "@test.example"); u.setPassword("unused");
        u.setFullName(name); u.setRole(role(name)); u.setSchool(s); u.setInstitutionId(s == null ? null : s.getId().toString());
        return users.saveAndFlush(u);
    }
    private String token(User u) { return "Bearer " + jwt.generateToken(u.getEmail(), u.getRole().getName()); }
    private String path(School s) { return "/api/schools/" + s.getId() + "/users"; }

    @Test void managerCanReadOwnSchoolButNotAnotherSchool() throws Exception {
        http.perform(get(path(school)).header("Authorization", token(manager))).andExpect(status().isOk());
        http.perform(get(path(school("B"))).header("Authorization", token(manager))).andExpect(status().isForbidden());
    }
    @Test void teacherCannotUseManagementApi() throws Exception {
        http.perform(get(path(school)).header("Authorization", token(user("TEACHER", school))))
                .andExpect(status().isForbidden());
    }
    @Test void accountDisableInvalidatesAlreadyIssuedJwt() throws Exception {
        String existing = token(manager); manager.setEnabled(false); users.saveAndFlush(manager);
        http.perform(get("/api/user/me").header("Authorization", existing)).andExpect(status().isUnauthorized());
    }
    @Test void schoolDisableInvalidatesAlreadyIssuedJwt() throws Exception {
        String existing = token(manager); school.setActive(false); schools.saveAndFlush(school);
        http.perform(get("/api/user/me").header("Authorization", existing)).andExpect(status().isUnauthorized());
    }
    @Test void expiredLicenseAllowsReadsButBlocksWrites() throws Exception {
        school.setLicenseEnd(LocalDate.now().minusDays(1)); schools.saveAndFlush(school);
        http.perform(get(path(school)).header("Authorization", token(manager))).andExpect(status().isOk());
        http.perform(post(path(school)).header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teacher@test.example\",\"password\":\"password123\",\"fullName\":\"Teacher\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
        http.perform(get("/api/user/me/license").header("Authorization", token(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canPerformWriteOperations").value(false));
    }
    @Test void expiredManagerCanReachRenewalQuote() throws Exception {
        school.setLicenseEnd(LocalDate.now().minusDays(1)); schools.saveAndFlush(school);
        http.perform(post("/api/school/billing/quote").header("Authorization", token(manager))
                .contentType(MediaType.APPLICATION_JSON).content("{\"planCode\":\"MISSING\"}"))
                .andExpect(status().isBadRequest()); // service reached, not rejected by license filter
    }
    @Test void managerCannotCreateAdminOrOverrideTargetSchool() throws Exception {
        http.perform(post(path(school)).header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"intruder@test.example\",\"password\":\"password123\",\"fullName\":\"Intruder\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        School other = school("B");
        http.perform(post(path(school)).header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"teacher@test.example\",\"password\":\"password123\",\"fullName\":\"Teacher\",\"role\":\"TEACHER\",\"institutionId\":\"" + other.getId() + "\"}"))
                .andExpect(status().isOk());
        assertThat(users.findByEmail("teacher@test.example").orElseThrow().getSchool().getId()).isEqualTo(school.getId());
    }
    @Test void ownProfileCannotChangeSchool() throws Exception {
        http.perform(put("/api/user/me").header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"institutionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void reviewerAliasCanAccessModuleReleasesButNotCreateProblems() throws Exception {
        User reviewer = user("CONTENT_REVIEWER", null);
        http.perform(get("/api/module-releases/" + UUID.randomUUID()).header("Authorization", token(reviewer))).andExpect(status().isNotFound());
        http.perform(post("/api/problems").header("Authorization", token(reviewer)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
    @Test void signupIsDisabledAndAnonymousSchoolApiIsProtected() throws Exception {
        http.perform(get(path(school))).andExpect(status().isUnauthorized());
        http.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@test.example\",\"password\":\"password123\",\"fullName\":\"New User\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void reportDoesNotCarryPreviousMonthUsageForward() throws Exception {
        school.setTokenUsageMonth(LocalDate.now().minusMonths(1).withDayOfMonth(1)); school.setUsedTokens(999L);
        schools.saveAndFlush(school);
        http.perform(get("/api/schools/" + school.getId() + "/reports/summary").header("Authorization", token(manager)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usedTokens").value(0));
    }

    @Test void classEnrollmentRejectsCrossSchoolAndTransfersWithoutDuplicateActiveYear() throws Exception {
        String root = "/api/schools/" + school.getId() + "/classes";
        String first = createClass(root, "10A1"); String second = createClass(root, "10A2");
        User student = user("STUDENT", school);
        User outsider = user("STUDENT", school("B"));
        http.perform(post(root + "/" + first + "/students").header("Authorization", token(manager))
                .contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":" + outsider.getId() + "}"))
                .andExpect(status().isBadRequest());
        http.perform(post(root + "/" + first + "/students").header("Authorization", token(manager))
                .contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":" + student.getId() + "}"))
                .andExpect(status().isOk());
        http.perform(post(root + "/" + second + "/students").header("Authorization", token(manager))
                .contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":" + student.getId() + "}"))
                .andExpect(status().isConflict());
        http.perform(put(root + "/" + second + "/students/" + student.getId() + "/transfer")
                .header("Authorization", token(manager))).andExpect(status().isOk());
        assertThat(enrollments.findActiveEnrollmentsByStudentId(student.getId()))
                .singleElement().satisfies(e -> assertThat(e.getSchoolClass().getId().toString()).isEqualTo(second));
    }

    @Test void classOverviewsAreRoleAndSchoolScoped() throws Exception {
        String root = "/api/schools/" + school.getId() + "/classes";
        String id = createClass(root, "10A1");
        User teacher = user("TEACHER", school);
        User student = user("STUDENT", school);
        http.perform(post(root + "/" + id + "/teachers").header("Authorization", token(manager))
                .contentType(MediaType.APPLICATION_JSON).content("{\"teacherId\":" + teacher.getId() + "}"))
                .andExpect(status().isOk());
        http.perform(post(root + "/" + id + "/students").header("Authorization", token(manager))
                .contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":" + student.getId() + "}"))
                .andExpect(status().isOk());
        http.perform(get("/api/student/classes").header("Authorization", token(student)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].teachers[0].id").value(teacher.getId()))
                .andExpect(jsonPath("$[0].classmateCount").value(0));
        http.perform(get("/api/assignments/mine/teacher/classes").header("Authorization", token(teacher)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].students[0].id").value(student.getId()));
        http.perform(get("/api/student/classes").header("Authorization", token(manager)))
                .andExpect(status().isForbidden());
        http.perform(get("/api/assignments/mine/teacher/classes").header("Authorization", token(student)))
                .andExpect(status().isForbidden());
        http.perform(get("/api/student/classes").header("Authorization", token(user("STUDENT", school("B")))))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        school.setLicenseEnd(LocalDate.now().minusDays(1)); schools.saveAndFlush(school);
        http.perform(get("/api/student/classes").header("Authorization", token(student))).andExpect(status().isOk());
    }

    @Autowired com.example.backend.repository.school.SchoolPaymentRepository payments;

    @Test void revenueQueryHandlesEmptyAndMixedStatuses() throws Exception {
        assertThat(payments.summarizeRevenue().getGrossPaidVnd()).isZero();
        for (String state : java.util.List.of("PAID", "PAID", "PENDING", "REQUIRES_REVIEW", "FAILED")) {
            var payment = new com.example.backend.entity.school.SchoolPayment();
            payment.setSchool(school); payment.setManager(manager); payment.setPlanCode("TEST");
            payment.setStatus(state); payment.setAmountVnd(100L); payments.saveAndFlush(payment);
        }
        var totals = payments.summarizeRevenue();
        assertThat(totals.getPaidTransactions()).isEqualTo(2L);
        assertThat(totals.getPendingTransactions()).isEqualTo(1L);
        assertThat(totals.getReviewTransactions()).isEqualTo(1L);
        assertThat(totals.getGrossPaidVnd()).isEqualTo(200L);
        http.perform(get("/api/admin/payment-report").header("Authorization", token(user("ADMIN", null))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grossPaidVnd").value(200));
    }

    private String createClass(String path, String name) throws Exception {
        String body = http.perform(post(path).header("Authorization", token(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"gradeLevel\":10,\"schoolYear\":\"2026-2027\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asText();
    }
}
