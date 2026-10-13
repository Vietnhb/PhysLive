package com.example.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.assignment.StudentActionLogRequest;
import com.example.backend.dto.assignment.StudentActionLogResponse;
import com.example.backend.service.StudentActionLogService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/action-logs")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentActionLogController {
    private final StudentActionLogService service;

    @PostMapping
    public StudentActionLogResponse create(@Valid @RequestBody StudentActionLogRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<StudentActionLogResponse> mine() {
        return service.mine();
    }
}
