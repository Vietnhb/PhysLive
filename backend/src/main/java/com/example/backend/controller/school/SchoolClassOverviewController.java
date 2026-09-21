package com.example.backend.controller.school;

import com.example.backend.service.school.SchoolClassService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SchoolClassOverviewController {
    private final SchoolClassService service;

    @GetMapping("/api/student/classes")
    @PreAuthorize("hasRole('STUDENT')")
    public List<SchoolClassService.StudentClassSummary> studentClasses() {
        return service.mineForStudent();
    }

    @GetMapping("/api/assignments/mine/teacher/classes")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<SchoolClassService.TeacherClassOption> teacherClasses() {
        return service.classesForTeacher();
    }
}
