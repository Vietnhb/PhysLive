package com.example.backend.controller.school;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.assignment.SchoolAssignmentResponse;
import com.example.backend.service.school.SchoolAssignmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schools/{schoolId}/assignments")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class SchoolAssignmentController {
    private final SchoolAssignmentService service;

    @GetMapping
    public Page<SchoolAssignmentResponse> list(@PathVariable UUID schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return service.list(schoolId, page, size);
    }
}
