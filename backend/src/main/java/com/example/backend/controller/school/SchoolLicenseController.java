package com.example.backend.controller.school;

import com.example.backend.dto.school.LicenseStatusResponse;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.school.LicenseCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SchoolLicenseController {
    private final LicenseCheckService licenses;
    private final CurrentUserService currentUser;

    @GetMapping("/api/user/me/license")
    public LicenseStatusResponse status() {
        return licenses.status(currentUser.requireCurrentUser());
    }
}
