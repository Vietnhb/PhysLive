package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.annotation.StandardApiResponses;
import com.example.backend.dto.ContentModuleRequest;
import com.example.backend.dto.ContentModuleResponse;
import com.example.backend.dto.GradeLevelResponse;
import com.example.backend.dto.LessonResponse;
import com.example.backend.dto.TopicRequest;
import com.example.backend.dto.TopicResponse;
import com.example.backend.service.CurriculumService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Tag(name = "Admin - Curriculum Management", description = "Quản lý và cấu hình chương trình học, chủ đề (Topics) và modules (Dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@StandardApiResponses
@RestController
@RequestMapping("/api/admin/curriculum")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminCurriculumController {

    private final CurriculumService curriculumService;

    // ------------------------------------------------------------------ //
    // Topics
    // ------------------------------------------------------------------ //

    @Operation(summary = "Danh sách tất cả các chủ đề (Topics)", description = "Lấy toàn bộ chủ đề vật lý (Kinematics, Dynamics, Circuits) sắp xếp theo thứ tự.")
    @GetMapping("/topics")
    public ResponseEntity<List<TopicResponse>> getAllTopics() {
        return ResponseEntity.ok(curriculumService.getAllTopics());
    }

    @Operation(summary = "Chi tiết chủ đề theo ID")
    @GetMapping("/topics/{id}")
    public ResponseEntity<TopicResponse> getTopicById(@PathVariable UUID id) {
        return ResponseEntity.ok(curriculumService.getTopicById(id));
    }

    @Operation(summary = "Tạo mới chủ đề (Topic)")
    @PostMapping("/topics")
    public ResponseEntity<TopicResponse> createTopic(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(curriculumService.createTopic(request));
    }

    @Operation(summary = "Cập nhật thông tin chủ đề (Topic)")
    @PutMapping("/topics/{id}")
    public ResponseEntity<TopicResponse> updateTopic(@PathVariable UUID id, @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.ok(curriculumService.updateTopic(id, request));
    }

    @Operation(summary = "Bật/Tắt kích hoạt chủ đề (Toggle Topic Enabled)", description = "Cho phép Admin tạm ẩn hoặc mở lại chủ đề vật lý cho trường học.")
    @PatchMapping("/topics/{id}/toggle")
    public ResponseEntity<TopicResponse> toggleTopic(@PathVariable UUID id) {
        return ResponseEntity.ok(curriculumService.toggleTopicEnabled(id));
    }

    @Operation(summary = "Xóa chủ đề (Topic)")
    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id) {
        curriculumService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ //
    // Content Modules
    // ------------------------------------------------------------------ //

    @Operation(summary = "Danh sách Module theo Topic ID")
    @GetMapping("/modules")
    public ResponseEntity<List<ContentModuleResponse>> getModulesByTopic(@RequestParam UUID topicId) {
        return ResponseEntity.ok(curriculumService.getModulesByTopic(topicId));
    }

    @Operation(summary = "Tạo mới Content Module")
    @PostMapping("/modules")
    public ResponseEntity<ContentModuleResponse> createModule(@Valid @RequestBody ContentModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(curriculumService.createModule(request));
    }

    @Operation(summary = "Cập nhật Content Module")
    @PutMapping("/modules/{id}")
    public ResponseEntity<ContentModuleResponse> updateModule(@PathVariable UUID id, @Valid @RequestBody ContentModuleRequest request) {
        return ResponseEntity.ok(curriculumService.updateModule(id, request));
    }

    @Operation(summary = "Bật/Tắt trạng thái hoạt động của Module")
    @PatchMapping("/modules/{id}/toggle")
    public ResponseEntity<ContentModuleResponse> toggleModule(@PathVariable UUID id) {
        return ResponseEntity.ok(curriculumService.toggleModuleActive(id));
    }

    // ------------------------------------------------------------------ //
    // Grade Levels & Lessons
    // ------------------------------------------------------------------ //

    @Operation(summary = "Danh sách Khối lớp (Grade Levels) theo Module ID")
    @GetMapping("/levels")
    public ResponseEntity<List<GradeLevelResponse>> getLevelsByModule(@RequestParam UUID moduleId) {
        return ResponseEntity.ok(curriculumService.getLevelsByModule(moduleId));
    }

    @Operation(summary = "Bật/Tắt trạng thái hoạt động của Khối lớp")
    @PatchMapping("/levels/{id}/toggle")
    public ResponseEntity<GradeLevelResponse> toggleLevel(@PathVariable UUID id) {
        return ResponseEntity.ok(curriculumService.toggleLevelActive(id));
    }

    @Operation(summary = "Danh sách Bài học (Lessons) theo Level ID")
    @GetMapping("/lessons")
    public ResponseEntity<List<LessonResponse>> getLessonsByLevel(@RequestParam UUID levelId) {
        return ResponseEntity.ok(curriculumService.getLessonsByLevel(levelId));
    }

    @Operation(summary = "Bật/Tắt trạng thái hoạt động của Bài học")
    @PatchMapping("/lessons/{id}/toggle")
    public ResponseEntity<LessonResponse> toggleLesson(@PathVariable UUID id) {
        return ResponseEntity.ok(curriculumService.toggleLessonActive(id));
    }
}
