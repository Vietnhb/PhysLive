package com.example.backend.controller.school;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.LibraryItemResponse;
import com.example.backend.dto.library.LibraryModerationContracts;
import com.example.backend.entity.enums.LibraryModerationStatus;
import com.example.backend.service.LibraryModerationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schools/{schoolId}/library")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class SchoolLibraryModerationController {
    private final LibraryModerationService service;

    @GetMapping
    public List<LibraryItemResponse> queue(@PathVariable UUID schoolId,
            @RequestParam(required = false) LibraryModerationStatus status) {
        return service.schoolQueue(schoolId, status);
    }

    @PutMapping("/{id}")
    public LibraryItemResponse moderate(@PathVariable UUID schoolId, @PathVariable UUID id,
            @Valid @RequestBody LibraryModerationContracts.Request request) {
        return service.moderateSchool(schoolId, id, request.status(), request.comment());
    }
}
