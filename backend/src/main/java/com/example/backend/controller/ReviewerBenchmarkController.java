package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.AdjudicateBenchmarkRequest;
import com.example.backend.dto.BenchmarkAdjudicationResponse;
import com.example.backend.dto.BenchmarkProblemRequest;
import com.example.backend.dto.BenchmarkProblemResponse;
import com.example.backend.service.ReviewerBenchmarkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Reviewer - Benchmark Corpus", description = "Quản lý tập bài toán chuẩn và trọng tài phân xử bất đồng annotator (Dành cho REVIEWER & ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/reviewer/benchmarks")
@PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
@AllArgsConstructor
public class ReviewerBenchmarkController {

    private final ReviewerBenchmarkService benchmarkService;

    @Operation(
        summary = "Danh sách bài toán chuẩn trong Benchmark Corpus",
        description = "Lấy danh sách các bài toán vật lý mẫu (Gold Standard Benchmark) theo chủ đề."
    )
    @GetMapping
    public ResponseEntity<List<BenchmarkProblemResponse>> getBenchmarkProblems(
            @Parameter(description = "Lọc theo chủ đề: kinematics, dynamics, circuits")
            @RequestParam(required = false) String topic) {
        return ResponseEntity.ok(benchmarkService.getBenchmarkProblems(topic));
    }

    @Operation(summary = "Chi tiết bài toán Benchmark theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<BenchmarkProblemResponse> getBenchmarkProblemById(@PathVariable UUID id) {
        return ResponseEntity.ok(benchmarkService.getBenchmarkProblemById(id));
    }

    @Operation(summary = "Thêm bài toán mới vào Benchmark Corpus")
    @PostMapping
    public ResponseEntity<BenchmarkProblemResponse> createBenchmarkProblem(
            @Valid @RequestBody BenchmarkProblemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(benchmarkService.createBenchmarkProblem(request));
    }

    @Operation(
        summary = "Trọng tài phân xử bất đồng Annotator (Adjudication)",
        description = "Chuyên gia thứ ba đưa ra đặc tả giải quyết cuối cùng (Gold Standard Specification) khi 2 giáo viên annotator bất đồng ý kiến."
    )
    @PostMapping("/{id}/adjudicate")
    public ResponseEntity<BenchmarkAdjudicationResponse> adjudicateBenchmark(
            @PathVariable UUID id,
            @Valid @RequestBody AdjudicateBenchmarkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(benchmarkService.adjudicate(id, request));
    }

    @Operation(summary = "Danh sách các phán quyết bất đồng của bài toán này")
    @GetMapping("/{id}/adjudications")
    public ResponseEntity<List<BenchmarkAdjudicationResponse>> getAdjudications(@PathVariable UUID id) {
        return ResponseEntity.ok(benchmarkService.getAdjudicationsByProblem(id));
    }
}
