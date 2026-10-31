package com.example.backend.system.school.controller;

import com.example.backend.system.school.dto.SchoolReportContracts;
import com.example.backend.system.school.service.SchoolReportService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schools/{schoolId}/reports")
@RequiredArgsConstructor
public class SchoolReportController {
    private final SchoolReportService service;

    @GetMapping("/summary")
    public SchoolReportContracts.Summary summary(@PathVariable UUID schoolId) {
        return service.summary(schoolId);
    }

    @GetMapping("/classes")
    public List<SchoolReportContracts.ClassRow> classes(@PathVariable UUID schoolId) {
        return service.classes(schoolId);
    }

    @GetMapping("/token-audit")
    public List<SchoolReportContracts.TokenRow> tokenAudit(@PathVariable UUID schoolId) {
        return service.tokenAudit(schoolId);
    }

    @GetMapping("/token-audit/page")
    public SchoolReportContracts.TokenPage tokenPage(@PathVariable UUID schoolId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String query, @RequestParam(required = false) String operation,
            @RequestParam(required = false) String month) {
        return service.tokenPage(schoolId, page, size, query, operation, month);
    }

    @GetMapping("/token-audit/options")
    public SchoolReportContracts.TokenOptions tokenOptions(@PathVariable UUID schoolId) {
        return service.tokenOptions(schoolId);
    }

    @GetMapping(value = "/classes.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportClasses(@PathVariable UUID schoolId,
            @RequestParam(required = false) Integer grade, @RequestParam(required = false) String year,
            @RequestParam(required = false) String query) {
        StringBuilder csv = new StringBuilder("\uFEFFname,gradeLevel,schoolYear,teachers,students\r\n");
        service.filteredClasses(schoolId, grade, year, query)
                .forEach(row -> csv.append(csv(row.name())).append(',').append(row.gradeLevel()).append(',')
                        .append(csv(row.schoolYear())).append(',').append(row.teachers()).append(',')
                        .append(row.students()).append("\r\n"));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+@-\t\r\n".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
