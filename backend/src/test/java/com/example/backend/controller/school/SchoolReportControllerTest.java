package com.example.backend.controller.school;

import com.example.backend.dto.school.SchoolReportContracts.ClassRow;
import com.example.backend.service.school.SchoolReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchoolReportControllerTest {

    @Test
    void csvExportIsExcelCompatibleAndNeutralizesFormulaCells() {
        UUID schoolId = UUID.randomUUID();
        SchoolReportService service = mock(SchoolReportService.class);
        when(service.classes(schoolId)).thenReturn(List.of(
                new ClassRow(UUID.randomUUID(), "=SUM(A1:A2)", 12, "2026-2027", 2, 30)));

        ResponseEntity<byte[]> response = new SchoolReportController(service).exportClasses(schoolId);
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFFname,gradeLevel,schoolYear,teachers,students\r\n");
        assertThat(csv).contains("\"'=SUM(A1:A2)\",12,\"2026-2027\",2,30\r\n");
        assertThat(csv.replace("\r\n", "")).doesNotContain("\n");
        assertThat(response.getHeaders().getContentType()).hasToString("text/csv;charset=UTF-8");
    }

    @Test
    void csvExportEscapesQuotesAndHandlesNullValues() {
        UUID schoolId = UUID.randomUUID();
        SchoolReportService service = mock(SchoolReportService.class);
        when(service.classes(schoolId)).thenReturn(List.of(
                new ClassRow(UUID.randomUUID(), "Physics \"Advanced\"", 11, null, 1, 20)));

        String csv = new String(new SchoolReportController(service).exportClasses(schoolId).getBody(),
                StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Physics \"\"Advanced\"\"\",11,\"\",1,20\r\n");
    }
}
