package com.example.backend.service.school;

import com.example.backend.entity.school.School;
import com.example.backend.entity.school.LicensePlan;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.school.SchoolRepository;
import com.example.backend.repository.school.LicensePlanRepository;
import com.example.backend.service.CurrentUserService;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolAdministrationService {
    private final SchoolRepository schools;
    private final LicensePlanRepository plans;
    private final CurrentUserService currentUser;

    public record SchoolRequest(@NotBlank @Size(max=200) String name,
            @NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9_-]+") String code,
            String address, @Email String contactEmail, String phoneNumber,
            LocalDate licenseStart, LocalDate licenseEnd,
            @PositiveOrZero Integer monthlyTokenQuota, @PositiveOrZero Integer studentQuota) { }
    public record PlanRequest(@NotBlank @Size(max=40) String code, @NotBlank String name,
            @NotBlank String description, @NotNull @Positive Long annualPriceVnd,
            @NotNull @Positive Integer studentQuota, @PositiveOrZero Integer monthlyTokenQuota, boolean active) { }

    @Transactional(readOnly=true)
    public List<School> list() { currentUser.requireAnyRole("ADMIN"); return schools.findAll(); }

    @Transactional
    public School save(UUID id, SchoolRequest request) {
        currentUser.requireAnyRole("ADMIN");
        if ((request.licenseStart() == null) != (request.licenseEnd() == null)
                || (request.licenseStart() != null && request.licenseEnd().isBefore(request.licenseStart())))
            throw new ApiException(HttpStatus.BAD_REQUEST, "License dates must form a valid range");
        School school = id == null ? new School() : requireSchool(id);
        school.setName(request.name().trim()); school.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        school.setAddress(request.address()); school.setContactEmail(request.contactEmail()); school.setPhoneNumber(request.phoneNumber());
        school.setLicenseStart(request.licenseStart()); school.setLicenseEnd(request.licenseEnd());
        school.setMonthlyTokenQuota(request.monthlyTokenQuota()); school.setStudentQuota(request.studentQuota());
        return schools.save(school);
    }

    @Transactional
    public School setActive(UUID id, boolean active) {
        currentUser.requireAnyRole("ADMIN"); School school = requireSchool(id);
        school.setActive(active); return schools.save(school);
    }

    @Transactional(readOnly=true)
    public List<LicensePlan> publicPlans() { return plans.findAll().stream().filter(LicensePlan::isActive).toList(); }

    @Transactional(readOnly=true)
    public List<LicensePlan> allPlans() { currentUser.requireAnyRole("ADMIN"); return plans.findAll(); }

    @Transactional
    public LicensePlan savePlan(PlanRequest request) {
        currentUser.requireAnyRole("ADMIN");
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        LicensePlan plan = plans.findById(code).orElseGet(LicensePlan::new);
        plan.setCode(code); plan.setName(request.name()); plan.setDescription(request.description());
        plan.setAnnualPriceVnd(request.annualPriceVnd()); plan.setStudentQuota(request.studentQuota());
        plan.setMonthlyTokenQuota(request.monthlyTokenQuota()); plan.setActive(request.active());
        return plans.save(plan);
    }

    private School requireSchool(UUID id) {
        return schools.findByIdForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "School not found"));
    }
}
