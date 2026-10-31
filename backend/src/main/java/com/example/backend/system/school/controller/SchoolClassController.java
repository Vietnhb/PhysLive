package com.example.backend.system.school.controller;

import com.example.backend.system.school.dto.SchoolClassContracts.AssignTeacherRequest;
import com.example.backend.system.school.dto.SchoolClassContracts.EnrollStudentRequest;
import com.example.backend.system.school.dto.SchoolClassContracts.SchoolClassRequest;
import com.example.backend.system.school.dto.SchoolClassContracts;
import com.example.backend.system.school.service.SchoolClassService;
import com.example.backend.system.school.dataio.service.SchoolClassCsv;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schools/{schoolId}/classes")
@RequiredArgsConstructor
public class SchoolClassController {
    private final SchoolClassService service;

    @GetMapping
    public List<SchoolClassContracts.ClassSummary> list(@PathVariable UUID schoolId) { return service.list(schoolId); }
    @GetMapping("/{classId}")
    public SchoolClassContracts.ClassDetail get(@PathVariable UUID schoolId, @PathVariable UUID classId) { return service.get(schoolId, classId); }
    @GetMapping(value = "/{classId}/students.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportStudents(@PathVariable UUID schoolId, @PathVariable UUID classId) {
        // Reuse the detail access check: the class must belong to a school the actor can read.
        var detail = service.get(schoolId, classId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("danh-sach-hoc-sinh-" + classId + ".csv").build().toString())
                .body(SchoolClassCsv.students(detail));
    }
    @PostMapping
    public SchoolClassContracts.ClassDetail create(@PathVariable UUID schoolId, @Valid @RequestBody SchoolClassRequest request) { return service.create(schoolId, request); }
    @PutMapping("/{classId}")
    public SchoolClassContracts.ClassDetail update(@PathVariable UUID schoolId, @PathVariable UUID classId, @Valid @RequestBody SchoolClassRequest request) { return service.update(schoolId, classId, request); }
    @DeleteMapping("/{classId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID schoolId, @PathVariable UUID classId) { service.archive(schoolId, classId); }
    @PostMapping("/{classId}/teachers")
    public SchoolClassContracts.TeacherAssignment assignTeacher(@PathVariable UUID schoolId, @PathVariable UUID classId, @Valid @RequestBody AssignTeacherRequest request) { return service.assignTeacher(schoolId, classId, request.teacherId()); }
    @DeleteMapping("/{classId}/teachers/{teacherId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void unassignTeacher(@PathVariable UUID schoolId, @PathVariable UUID classId, @PathVariable Integer teacherId) { service.unassignTeacher(schoolId, classId, teacherId); }
    @PostMapping("/{classId}/students")
    public SchoolClassContracts.Enrollment enroll(@PathVariable UUID schoolId, @PathVariable UUID classId, @Valid @RequestBody EnrollStudentRequest request) { return service.enrollStudent(schoolId, classId, request.studentId()); }
    @PutMapping("/{classId}/students/{studentId}/transfer")
    public SchoolClassContracts.Enrollment transfer(@PathVariable UUID schoolId, @PathVariable UUID classId, @PathVariable Integer studentId) { return service.transferStudent(schoolId, classId, studentId); }
    @DeleteMapping("/{classId}/students/{studentId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID schoolId, @PathVariable UUID classId, @PathVariable Integer studentId) { service.removeStudent(schoolId, classId, studentId); }
}
