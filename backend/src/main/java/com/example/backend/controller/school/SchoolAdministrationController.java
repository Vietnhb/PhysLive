package com.example.backend.controller.school;

import com.example.backend.entity.school.School;
import com.example.backend.entity.school.LicensePlan;
import com.example.backend.service.school.SchoolAdministrationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SchoolAdministrationController {
    private final SchoolAdministrationService service;
    @GetMapping("/api/admin/schools")
    public List<School> schools() { return service.list(); }
    @PostMapping("/api/admin/schools")
    public School create(@Valid @RequestBody SchoolAdministrationService.SchoolRequest request) { return service.save(null, request); }
    @PutMapping("/api/admin/schools/{id}")
    public School update(@PathVariable UUID id, @Valid @RequestBody SchoolAdministrationService.SchoolRequest request) { return service.save(id, request); }
    @PutMapping("/api/admin/schools/{id}/activate")
    public School activate(@PathVariable UUID id) { return service.setActive(id, true); }
    @PutMapping("/api/admin/schools/{id}/deactivate")
    public School deactivate(@PathVariable UUID id) { return service.setActive(id, false); }
    @GetMapping("/api/auth/plans")
    public List<LicensePlan> plans() { return service.publicPlans(); }
    @GetMapping("/api/admin/plans")
    public List<LicensePlan> adminPlans() { return service.allPlans(); }
    @PutMapping("/api/admin/plans")
    public LicensePlan savePlan(@Valid @RequestBody SchoolAdministrationService.PlanRequest request) { return service.savePlan(request); }
}
